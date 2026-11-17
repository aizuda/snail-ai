package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.vo.agent.AgentInstructionOptimizeRequestVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentInstructionOptimizeResponseVO;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentInstructionOptimizeServiceTest {

    private static final long MODEL_ID = 11L;
    private static final String ORIGINAL_INSTRUCTION = "你是一名客服助手。";

    @Test
    void shouldReturnOptimizedInstruction() {
        CapturingModelRuntimeHandler runtimeHandler = new CapturingModelRuntimeHandler("你是一名严谨的售后客服助手。");
        AgentInstructionOptimizeService service = service(new FixedModelConfigHandler(MODEL_ID), runtimeHandler);

        AgentInstructionOptimizeResponseVO response = service.optimize(request());

        assertEquals("你是一名严谨的售后客服助手。", response.getInstruction());
        assertTrue(response.getWarnings().isEmpty());
        assertEquals(MODEL_ID, runtimeHandler.request.modelConfigId());
        assertTrue(runtimeHandler.request.userContext().contains(ORIGINAL_INSTRUCTION));
    }

    @Test
    void shouldStripMarkdownFence() {
        CapturingModelRuntimeHandler runtimeHandler = new CapturingModelRuntimeHandler("""
                ```text
                你是一名售后客服助手。
                回答前先确认订单信息。
                ```
                """);
        AgentInstructionOptimizeService service = service(new FixedModelConfigHandler(MODEL_ID), runtimeHandler);

        AgentInstructionOptimizeResponseVO response = service.optimize(request());

        assertEquals("你是一名售后客服助手。\n回答前先确认订单信息。", response.getInstruction());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    void shouldFallBackWhenDefaultModelIsUnavailable() {
        AgentInstructionOptimizeService service = service(new FixedModelConfigHandler(null),
                new CapturingModelRuntimeHandler("unused"));

        AgentInstructionOptimizeResponseVO response = service.optimize(request());

        assertFallback(response);
    }

    @Test
    void shouldFallBackWhenModelCallFails() {
        AgentInstructionOptimizeService service = service(new FixedModelConfigHandler(MODEL_ID),
                new CapturingModelRuntimeHandler(new IllegalStateException("model unavailable")));

        AgentInstructionOptimizeResponseVO response = service.optimize(request());

        assertFallback(response);
    }

    @Test
    void shouldFallBackWhenModelReturnsBlankContent() {
        AgentInstructionOptimizeService service = service(new FixedModelConfigHandler(MODEL_ID),
                new CapturingModelRuntimeHandler("   "));

        AgentInstructionOptimizeResponseVO response = service.optimize(request());

        assertFallback(response);
    }

    private static AgentInstructionOptimizeService service(
            ModelConfigHandler configHandler,
            ModelRuntimeHandler runtimeHandler) {
        return new AgentInstructionOptimizeService(configHandler, runtimeHandler);
    }

    private static AgentInstructionOptimizeRequestVO request() {
        AgentInstructionOptimizeRequestVO request = new AgentInstructionOptimizeRequestVO();
        request.setName("客服助手");
        request.setDescription("处理售后问题");
        request.setInstruction(ORIGINAL_INSTRUCTION);
        request.setOptimizeInstruction("强化边界和回答格式");
        return request;
    }

    private static void assertFallback(AgentInstructionOptimizeResponseVO response) {
        assertEquals(ORIGINAL_INSTRUCTION, response.getInstruction());
        assertEquals(List.of(AgentInstructionOptimizeService.WARNING_AI_OPTIMIZE_FALLBACK), response.getWarnings());
    }

    private static class FixedModelConfigHandler extends ModelConfigHandler {

        private final Long modelId;

        FixedModelConfigHandler(Long modelId) {
            super(null, null, null);
            this.modelId = modelId;
        }

        @Override
        public ModelConfigInfoDTO getDefaultModelByType(String modelType) {
            return modelId == null ? null : ModelConfigInfoDTO.builder().id(modelId).build();
        }
    }

    private static class CapturingModelRuntimeHandler extends ModelRuntimeHandler {

        private final String result;
        private final RuntimeException failure;
        private ModelRuntimeHandler.ChatRequest request;

        CapturingModelRuntimeHandler(String result) {
            this(result, null);
        }

        CapturingModelRuntimeHandler(RuntimeException failure) {
            this(null, failure);
        }

        private CapturingModelRuntimeHandler(String result, RuntimeException failure) {
            super(null, null, null, null);
            this.result = result;
            this.failure = failure;
        }

        @Override
        public String chat(ModelRuntimeHandler.ChatRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
