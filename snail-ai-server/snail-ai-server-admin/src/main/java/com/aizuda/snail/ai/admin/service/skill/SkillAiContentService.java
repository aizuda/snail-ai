package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateResponseVO;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAiContentService {

    private static final int AI_TEXT_LIMIT = 6000;
    private static final String DEFAULT_SKILL_NAME = "new-skill";
    private static final String DEFAULT_SKILL_DESCRIPTION = "AI generated skill";
    private static final String WARNING_GENERATE_FALLBACK = "AI 生成失败，已使用规则草稿";
    private static final String WARNING_OPTIMIZE_FALLBACK = "AI 优化失败，已保留当前内容";

    private final ModelConfigHandler modelConfigHandler;
    private final ModelRuntimeHandler modelRuntimeHandler;

    public SkillAiGenerateResponseVO generateDraft(SkillAiGenerateRequestVO request) {
        try {
            String skillMd = generateSkillMdByModel(request, null, false);
            return responseFromSkillMd(request, skillMd, List.of());
        } catch (Exception e) {
            log.warn("Generate skill draft by AI failed, fallback to rule template: {}", e.getMessage());
            return buildRuleBasedDraft(request, List.of(WARNING_GENERATE_FALLBACK));
        }
    }

    public SkillAiGenerateResponseVO optimizeDraft(SkillAiGenerateRequestVO request, String currentContent) {
        String current = StrUtil.blankToDefault(currentContent, request.getCurrentContent());
        try {
            String skillMd = generateSkillMdByModel(request, current, true);
            return responseFromSkillMd(request, skillMd, List.of());
        } catch (Exception e) {
            log.warn("Optimize skill draft by AI failed, keep current content: {}", e.getMessage());
            return responseFromSkillMd(request, current, List.of(WARNING_OPTIMIZE_FALLBACK));
        }
    }

    private SkillAiGenerateResponseVO buildRuleBasedDraft(SkillAiGenerateRequestVO request, List<String> warnings) {
        String name = StrUtil.blankToDefault(request.getName(), DEFAULT_SKILL_NAME).trim();
        String description = StrUtil.blankToDefault(request.getDescription(), request.getRequirement()).trim();
        String body = """
                # %s

                ## 适用场景

                %s

                ## 工作流程

                1. 判断用户任务是否匹配本 Skill 的适用场景。
                2. 收集完成任务所需的输入、上下文和约束。
                3. 按步骤执行，并在缺少关键信息时向用户确认。
                4. 输出结构化结果，明确事实、推断和后续建议。
                """.formatted(name, request.getRequirement().trim());
        return SkillAiGenerateResponseVO.builder()
                .name(name)
                .description(description)
                .skillContent(rebuildSkillMd(name, description, body))
                .warnings(warnings)
                .build();
    }

    private String generateSkillMdByModel(SkillAiGenerateRequestVO request, String currentContent, boolean optimize) {
        if (modelConfigHandler == null || modelRuntimeHandler == null) {
            throw new IllegalStateException("模型运行组件未启用");
        }
        ModelConfigInfoDTO model = modelConfigHandler.getDefaultModelByType(ModelAdapterDefaults.CHAT_MODEL_TYPE);
        if (model == null || model.getId() == null) {
            throw new IllegalStateException("未配置默认 CHAT 模型");
        }
        String raw = modelRuntimeHandler.chat(new ModelRuntimeHandler.ChatRequest(
                model.getId(),
                buildSkillAiUserInput(request, currentContent, optimize),
                buildSkillAiSystemPrompt()));
        String skillMd = normalizeSkillAiOutput(raw);
        if (StrUtil.isBlank(skillMd)) {
            throw new IllegalStateException("模型返回内容为空");
        }
        return skillMd;
    }

    private String buildSkillAiSystemPrompt() {
        return """
                你是 Snail AI 的 Agent Skill 设计助手。
                只输出完整 SKILL.md 文件内容，不要解释，不要包含额外说明。
                输出必须包含 YAML frontmatter，至少包含 name 和 description。
                正文用 Markdown 编写，描述触发条件、工作流程、输入要求、输出格式、边界和失败处理。
                """;
    }

    private String buildSkillAiUserInput(SkillAiGenerateRequestVO request, String currentContent, boolean optimize) {
        return """
                模式：%s
                名称：%s
                描述：%s
                需求：%s
                优化方向：%s
                当前 SKILL.md：
                %s
                """.formatted(
                optimize ? "优化现有 Skill" : "生成新 Skill",
                StrUtil.blankToDefault(request.getName(), DEFAULT_SKILL_NAME),
                StrUtil.blankToDefault(request.getDescription(), ""),
                request.getRequirement(),
                StrUtil.blankToDefault(request.getOptimizeInstruction(), ""),
                truncateForAi(StrUtil.blankToDefault(currentContent, request.getCurrentContent())));
    }

    private SkillAiGenerateResponseVO responseFromSkillMd(
            SkillAiGenerateRequestVO request,
            String skillMd,
            List<String> warnings) {
        String content = StrUtil.nullToDefault(skillMd, "");
        SkillMarkdownParser parser = new SkillMarkdownParser();
        Map<String, Object> frontmatter = parser.parseFrontmatter(content);
        return SkillAiGenerateResponseVO.builder()
                .name(resolveFrontmatterValue(frontmatter, "name", request.getName()))
                .description(resolveFrontmatterValue(frontmatter, "description", request.getDescription()))
                .skillContent(content)
                .warnings(warnings)
                .build();
    }

    private String normalizeSkillAiOutput(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd > 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        if (!text.startsWith("---")) {
            return rebuildSkillMd(DEFAULT_SKILL_NAME, DEFAULT_SKILL_DESCRIPTION, text);
        }
        return text;
    }

    private String rebuildSkillMd(String name, String description, String skillContent) {
        return "---\n"
                + "name: " + StrUtil.nullToDefault(name, "") + "\n"
                + "description: \"" + escapeYamlString(description) + "\"\n"
                + "---\n"
                + StrUtil.nullToDefault(skillContent, "");
    }

    private String escapeYamlString(String value) {
        return StrUtil.nullToDefault(value, "").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveFrontmatterValue(Map<String, Object> frontmatter, String key, String fallback) {
        if (frontmatter == null || !frontmatter.containsKey(key)) {
            return fallback;
        }
        return StrUtil.toStringOrNull(frontmatter.get(key));
    }

    private String truncateForAi(String value) {
        String text = StrUtil.blankToDefault(value, "");
        return text.length() <= AI_TEXT_LIMIT ? text : text.substring(0, AI_TEXT_LIMIT);
    }
}
