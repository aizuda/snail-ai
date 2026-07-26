package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;

import java.util.List;

public class LlmCallHandlerTest {

    public void testShouldIncludeResolvedAttachmentsInDispatchRequest() {
        LlmCallHandler handler = new LlmCallHandler(null);
        AgentPO agent = AgentPO.builder()
                .id(1L)
                .name("图片助手")
                .build();
        AgentChatContext context = new AgentChatContext(1L, "conversation-1", "看图说明", null, null, null);
        context.setAgent(agent);
        context.setModelConfig(ChatDispatchRequest.ModelConfig.builder().modelConfigId(2L).build());
        context.setAttachments(List.of(AgentChatAttachmentDTO.builder()
                .resourceId(12L)
                .type(AgentChatAttachmentTypeEnum.IMAGE.getValue())
                .fileName("screen.png")
                .mimeType("image/png")
                .size(128L)
                .accessUrl("/resource/12")
                .base64("base64")
                .build()));

        ChatDispatchRequest request = handler.buildDispatchRequest(context);

        assertEquals(1, request.getMediaAttachments().size());
        assertEquals(12L, request.getMediaAttachments().get(0).getResourceId());
        assertEquals("base64", request.getMediaAttachments().get(0).getBase64());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
