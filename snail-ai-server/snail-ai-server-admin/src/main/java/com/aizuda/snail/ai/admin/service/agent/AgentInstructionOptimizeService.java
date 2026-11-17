package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.agent.AgentInstructionOptimizeRequestVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentInstructionOptimizeResponseVO;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** 使用默认 CHAT 模型生成系统提示词候选版本。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentInstructionOptimizeService {

    public static final String WARNING_AI_OPTIMIZE_FALLBACK = "AI_OPTIMIZE_FALLBACK";

    private static final int MAX_INPUT_LENGTH = 12000;

    private final ModelConfigHandler modelConfigHandler;
    private final ModelRuntimeHandler modelRuntimeHandler;

    public AgentInstructionOptimizeResponseVO optimize(AgentInstructionOptimizeRequestVO request) {
        String originalInstruction = request.getInstruction();
        try {
            String optimizedInstruction = optimizeByModel(request);
            return response(optimizedInstruction, List.of());
        } catch (Exception e) {
            log.warn("Optimize agent instruction by AI failed, keep original instruction: {}", e.getMessage());
            return response(originalInstruction, List.of(WARNING_AI_OPTIMIZE_FALLBACK));
        }
    }

    private String optimizeByModel(AgentInstructionOptimizeRequestVO request) {
        ModelConfigInfoDTO model = modelConfigHandler.getDefaultModelByType(ModelAdapterDefaults.CHAT_MODEL_TYPE);
        if (model == null || model.getId() == null) {
            throw new IllegalStateException("未配置默认 CHAT 模型");
        }

        String raw = modelRuntimeHandler.chat(new ModelRuntimeHandler.ChatRequest(
                model.getId(), buildUserInput(request), buildSystemPrompt()));
        String instruction = normalizeModelOutput(raw);
        if (StrUtil.isBlank(instruction)) {
            throw new IllegalStateException("模型返回内容为空");
        }
        return instruction;
    }

    private String buildSystemPrompt() {
        return """
                你是专业的 AI 智能体系统提示词设计师。
                请优化用户提供的系统提示词，使角色、目标、行为边界、回答流程和输出格式清晰可执行。
                保留原提示词中的业务事实和必要约束，不添加未经提供的业务承诺。
                只输出优化后的系统提示词正文，不要解释，不要添加前言或结语，不要使用 Markdown 代码围栏。
                """;
    }

    private String buildUserInput(AgentInstructionOptimizeRequestVO request) {
        return """
                智能体名称：%s
                智能体描述：%s
                优化方向：%s

                当前系统提示词：
                %s
                """.formatted(
                truncate(request.getName()),
                truncate(StrUtil.nullToDefault(request.getDescription(), "")),
                truncate(StrUtil.nullToDefault(request.getOptimizeInstruction(), "")),
                truncate(request.getInstruction()));
    }

    private String normalizeModelOutput(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return text;
        }
        return text.substring(firstLineEnd + 1, lastFence).trim();
    }

    private String truncate(String value) {
        String text = StrUtil.nullToDefault(value, "");
        return text.length() <= MAX_INPUT_LENGTH ? text : text.substring(0, MAX_INPUT_LENGTH);
    }

    private AgentInstructionOptimizeResponseVO response(String instruction, List<String> warnings) {
        return AgentInstructionOptimizeResponseVO.builder()
                .instruction(instruction)
                .warnings(warnings)
                .build();
    }
}
