package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.AgentChatChainService;
import com.aizuda.snail.ai.admin.service.agent.AgentChatService;
import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatAttachmentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatStreamEvent;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        CapturingAgentChatService agentChatService = new CapturingAgentChatService(null);
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

    @Test
    void chatSyncShouldReturnReportedUsage() {
        OpenApiChatService service = createService(ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", "", 120, 40, 20, 500L));

        OpenApiChatSyncResponse response = service.chatSync(createRequest());

        assertEquals(120, response.getUsage().getPromptTokens());
        assertEquals(40, response.getUsage().getCompletionTokens());
        assertEquals(20, response.getUsage().getCacheTokens());
        assertEquals(160, response.getUsage().getTotalTokens());
    }

    @Test
    void chatSyncShouldSerializeMissingUsageAsNull() {
        OpenApiChatService service = createService(ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", "", 0, 0, 0, 500L));

        OpenApiChatSyncResponse response = service.chatSync(createRequest());

        assertNull(response.getUsage());
        assertTrue(JsonUtil.toJsonString(response).contains("\"usage\":null"));
    }

    @Test
    void chatStreamDoneShouldContainUsage() {
        OpenApiChatService service = createService(ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", "", 12, 8, null, 500L));

        List<OpenApiChatStreamEvent> events = service.chatStream(createRequest()).collectList().block();

        OpenApiChatStreamEvent done = events.stream()
                .filter(event -> OpenApiChatStreamEvent.TYPE_DONE.equals(event.getType()))
                .findFirst()
                .orElseThrow();
        assertTrue(done.getData().contains("\"promptTokens\":12"));
        assertTrue(done.getData().contains("\"completionTokens\":8"));
        assertTrue(done.getData().contains("\"cacheTokens\":0"));
        assertTrue(done.getData().contains("\"totalTokens\":20"));
    }

    @Test
    void chatStreamDoneShouldSerializeMissingUsageAsNull() {
        OpenApiChatService service = createService(null);

        List<OpenApiChatStreamEvent> events = service.chatStream(createRequest()).collectList().block();

        OpenApiChatStreamEvent done = events.stream()
                .filter(event -> OpenApiChatStreamEvent.TYPE_DONE.equals(event.getType()))
                .findFirst()
                .orElseThrow();
        assertTrue(done.getData().contains("\"usage\":null"));
    }

    @Test
    void chatSyncShouldPropagateExecutionError() {
        OpenApiChatService service = createFailingService();

        SnailAiException exception = assertThrows(SnailAiException.class, () -> service.chatSync(createRequest()));

        assertTrue(exception.getMessage().contains("model unavailable"));
    }

    @Test
    void chatStreamShouldEmitErrorWithoutDone() {
        OpenApiChatService service = createFailingService();

        List<OpenApiChatStreamEvent> events = service.chatStream(createRequest()).collectList().block();

        assertEquals(1, events.size());
        assertEquals(OpenApiChatStreamEvent.TYPE_ERROR, events.getFirst().getType());
        assertTrue(events.getFirst().getData().contains("model unavailable"));
    }

    private OpenApiChatService createService(ChatStreamResponse completion) {
        OpenApiSessionUtils.set(OpenApiSessionUtils.OpenApiSession.builder()
                .appId("app-1")
                .build());
        return new OpenApiChatService(
                new FakeOpenApiUserResolver(),
                new CapturingAgentChatService(completion));
    }

    private OpenApiChatService createFailingService() {
        OpenApiSessionUtils.set(OpenApiSessionUtils.OpenApiSession.builder()
                .appId("app-1")
                .build());
        return new OpenApiChatService(
                new FakeOpenApiUserResolver(),
                new FailingAgentChatService());
    }

    private OpenApiChatRequest createRequest() {
        OpenApiChatRequest request = new OpenApiChatRequest();
        request.setAgentId(1L);
        request.setConversationId("conversation-1");
        request.setOpenId("open-1");
        request.setContent("hello");
        return request;
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
        private final ChatStreamResponse completion;

        private CapturingAgentChatService(ChatStreamResponse completion) {
            super(new AgentChatChainService(List.of()));
            this.completion = completion;
        }

        @Override
        public void chat(AgentChatCommand command) {
            this.command = command;
            command.getStreamWriter().send(JsonUtil.toJsonString(ChatStreamResponse.text("answer")) + "\n");
            if (completion != null) {
                command.getStreamWriter().onCompletion(completion);
            }
            command.getStreamWriter().complete();
        }
    }

    private static class FailingAgentChatService extends AgentChatService {

        private FailingAgentChatService() {
            super(new AgentChatChainService(List.of()));
        }

        @Override
        public void chat(AgentChatCommand command) {
            command.getStreamWriter().completeWithError(new IllegalStateException("model unavailable"));
        }
    }
}
