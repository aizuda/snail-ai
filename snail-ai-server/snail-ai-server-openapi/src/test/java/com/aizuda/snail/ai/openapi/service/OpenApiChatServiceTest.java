package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.AgentChatChainService;
import com.aizuda.snail.ai.admin.service.agent.AgentChatService;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatAttachmentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiChatServiceTest {

    @AfterEach
    void tearDown() {
        OpenApiSessionUtils.clear();
    }

    @Test
    void chatSyncShouldPassAttachmentsToAgentChatCommand() {
        OpenApiSessionUtils.set(OpenApiSessionUtils.OpenApiSession.builder()
                .appId("app-1")
                .build());
        CapturingAgentChatService agentChatService = new CapturingAgentChatService();
        OpenApiChatService service = new OpenApiChatService(new FakeOpenApiUserResolver(), agentChatService);

        OpenApiChatAttachmentRequest attachment = new OpenApiChatAttachmentRequest();
        attachment.setResourceId(12L);
        attachment.setType(AgentChatAttachmentTypeEnum.IMAGE.getValue());
        OpenApiChatRequest request = new OpenApiChatRequest();
        request.setAgentId(1L);
        request.setConversationId("conversation-1");
        request.setOpenId("open-1");
        request.setContent("analyze image");
        request.setAttachments(List.of(attachment));

        service.chatSync(request);

        AgentChatCommand command = agentChatService.command;
        assertEquals(1, command.getAttachments().size());
        assertEquals(12L, command.getAttachments().get(0).getResourceId());
        assertEquals(AgentChatAttachmentTypeEnum.IMAGE.getValue(), command.getAttachments().get(0).getType());
    }

    private static class FakeOpenApiUserResolver extends OpenApiUserResolver {

        private FakeOpenApiUserResolver() {
            super(null, null);
        }

        @Override
        public UserPO resolvePlatformUser(String appId, String openId) {
            UserPO user = new UserPO();
            user.setId(7L);
            return user;
        }
    }

    private static class CapturingAgentChatService extends AgentChatService {

        private AgentChatCommand command;

        private CapturingAgentChatService() {
            super(new AgentChatChainService(List.of()));
        }

        @Override
        public void chat(AgentChatCommand command) {
            this.command = command;
            command.getStreamWriter().complete();
        }
    }
}
