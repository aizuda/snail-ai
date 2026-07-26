package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.vo.agent.AgentChatAttachmentVO;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;

import java.util.List;

public class AgentChatServiceTest {

    public void testShouldCopyRequestAttachmentsToChatContext() {
        CapturingAgentChatChainService chainService = new CapturingAgentChatChainService();
        AgentChatService service = new AgentChatService(chainService);
        AgentChatAttachmentVO attachment = new AgentChatAttachmentVO();
        attachment.setResourceId(12L);
        attachment.setType(AgentChatAttachmentTypeEnum.IMAGE.getValue());

        service.chat(AgentChatCommand.builder()
                .agentId(1L)
                .conversationId("conversation-1")
                .content("分析这张图片")
                .attachments(List.of(attachment))
                .build());

        AgentChatContext context = chainService.context;
        assertEquals(1, context.getRequestedAttachments().size());
        assertEquals(12L, context.getRequestedAttachments().get(0).getResourceId());
        assertEquals(AgentChatAttachmentTypeEnum.IMAGE.getValue(), context.getRequestedAttachments().get(0).getType());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static class CapturingAgentChatChainService extends AgentChatChainService {
        private AgentChatContext context;

        private CapturingAgentChatChainService() {
            super(List.of());
        }

        @Override
        public void proceed(AgentChatContext ctx) {
            this.context = ctx;
        }
    }
}
