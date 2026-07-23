package com.aizuda.snail.ai.admin.service.skill;

import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateResponseVO;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillAiContentServiceTest {

    private static final long MODEL_ID = 9L;

    @Test
    void generateDraftFallsBackToRuleBasedSkillWhenModelIsUnavailable() {
        SkillAiContentService service = new SkillAiContentService(null, null);
        SkillAiGenerateRequestVO request = request("email-summary", "总结邮件", "读取邮件内容并输出待办事项");

        SkillAiGenerateResponseVO response = service.generateDraft(request);

        assertEquals("email-summary", response.getName());
        assertEquals("总结邮件", response.getDescription());
        assertTrue(response.getSkillContent().startsWith("---\nname: email-summary\n"));
        assertTrue(response.getSkillContent().contains("读取邮件内容并输出待办事项"));
        assertFalse(response.getWarnings().isEmpty());
    }

    @Test
    void generateDraftNormalizesFencedMarkdownReturnedByModel() {
        String generated = """
                ```markdown
                ---
                name: generated-skill
                description: 生成描述
                ---
                # 工作流程
                ```
                """;
        CapturingModelRuntimeHandler runtimeHandler = new CapturingModelRuntimeHandler(generated);
        SkillAiContentService service = new SkillAiContentService(
                new FixedModelConfigHandler(), runtimeHandler);

        SkillAiGenerateResponseVO response = service.generateDraft(
                request("ignored", "ignored description", "生成一个技能"));

        assertEquals("""
                ---
                name: generated-skill
                description: 生成描述
                ---
                # 工作流程""", response.getSkillContent());
        assertEquals(MODEL_ID, runtimeHandler.request.modelConfigId());
    }

    @Test
    void optimizeDraftReturnsCurrentContentWhenModelIsUnavailable() {
        String current = """
                ---
                name: current-skill
                description: 当前技能
                ---
                # 原内容
                """;
        SkillAiContentService service = new SkillAiContentService(null, null);
        SkillAiGenerateRequestVO request = request("current-skill", "当前技能", "优化表达");
        request.setCurrentContent(current);
        request.setOptimizeInstruction("更清晰");

        SkillAiGenerateResponseVO response = service.optimizeDraft(request, current);

        assertEquals(current, response.getSkillContent());
        assertFalse(response.getWarnings().isEmpty());
    }

    private SkillAiGenerateRequestVO request(String name, String description, String requirement) {
        SkillAiGenerateRequestVO request = new SkillAiGenerateRequestVO();
        request.setName(name);
        request.setDescription(description);
        request.setRequirement(requirement);
        return request;
    }

    private static class FixedModelConfigHandler extends ModelConfigHandler {

        FixedModelConfigHandler() {
            super(null, null, null);
        }

        @Override
        public ModelConfigInfoDTO getDefaultModelByType(String modelType) {
            return ModelConfigInfoDTO.builder()
                    .id(MODEL_ID)
                    .build();
        }
    }

    private static class CapturingModelRuntimeHandler extends ModelRuntimeHandler {

        private final String result;
        private ModelRuntimeHandler.ChatRequest request;

        CapturingModelRuntimeHandler(String result) {
            super(null, null, null, null);
            this.result = result;
        }

        @Override
        public String chat(ModelRuntimeHandler.ChatRequest request) {
            this.request = request;
            return result;
        }
    }
}
