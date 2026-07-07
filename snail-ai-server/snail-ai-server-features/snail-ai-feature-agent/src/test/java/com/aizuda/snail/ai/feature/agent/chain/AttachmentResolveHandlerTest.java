package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;
import com.aizuda.snail.ai.feature.agent.service.AgentChatAttachmentService;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;

import java.lang.reflect.Constructor;
import java.util.List;

public class AttachmentResolveHandlerTest {

    public static void main(String[] args) {
        AttachmentResolveHandlerTest test = new AttachmentResolveHandlerTest();
        test.testShouldExposeSingleConstructorForSpringInjection();
        test.testShouldResolveRequestedAttachmentsAfterModelConfigReady();
    }

    public void testShouldExposeSingleConstructorForSpringInjection() {
        Constructor<?>[] constructors = AttachmentResolveHandler.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertEquals(2, constructors[0].getParameterCount());
    }

    public void testShouldResolveRequestedAttachmentsAfterModelConfigReady() {
        FakeAgentChatAttachmentService service = new FakeAgentChatAttachmentService();
        AttachmentResolveHandler handler = new AttachmentResolveHandler(service, null);
        AgentChatContext context = new AgentChatContext(1L, "conversation-1", "看图说明", null, null, null);
        UserPO user = new UserPO();
        user.setId(7L);
        context.setUser(user);
        context.setModelConfig(ChatDispatchRequest.ModelConfig.builder().modelConfigId(2L).build());
        context.setRequestedAttachments(List.of(AgentChatAttachmentDTO.builder()
                .resourceId(12L)
                .type(AgentChatAttachmentTypeEnum.IMAGE.getValue())
                .build()));

        handler.handle(context);

        assertEquals(1, context.getAttachments().size());
        assertEquals("screen.png", context.getAttachments().get(0).getFileName());
        assertEquals("base64", context.getAttachments().get(0).getBase64());
        assertEquals(context.getUser(), service.request.getUser());
        assertEquals(context.getModelConfig(), service.request.getModelConfig());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static class FakeAgentChatAttachmentService extends AgentChatAttachmentService {
        private ResolveRequest request;

        private FakeAgentChatAttachmentService() {
            super(null);
        }

        @Override
        public List<AgentChatAttachmentDTO> resolve(ResolveRequest request) {
            this.request = request;
            return List.of(AgentChatAttachmentDTO.builder()
                    .resourceId(12L)
                    .type(AgentChatAttachmentTypeEnum.IMAGE.getValue())
                    .fileName("screen.png")
                    .mimeType("image/png")
                    .size(128L)
                    .accessUrl("/resource/12")
                    .base64("base64")
                    .build());
        }
    }
}
