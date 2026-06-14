package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.AgentChatService;
import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatStreamEvent;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.openapi.stream.CollectingChatStreamWriter;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * OpenAPI 对话服务
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiChatService {

    private static final long SYNC_CHAT_TIMEOUT_MS = 300_000L;
    private final OpenApiUserResolver openApiUserResolver;
    private final AgentChatService agentChatService;

    /**
     * 流式对话 — 返回 OpenAPI 事件流
     */
    public Flux<OpenApiChatStreamEvent> chatStream(OpenApiChatRequest request) {
        UserPO requestUser = resolveRequestUser(request.getOpenId());

        String sid = StringUtils.hasText(request.getSid())
                ? request.getSid()
                : UUID.randomUUID().toString();

        return Flux.create(sink -> {
            ChatStreamState state = new ChatStreamState(request.getConversationId());

            Thread.startVirtualThread(() -> {
                try {
                    agentChatService.chat(AgentChatCommand.builder()
                            .agentId(request.getAgentId())
                            .conversationId(request.getConversationId())
                            .content(request.getContent())
                            .disabledMcpServerIds(request.getDisabledMcpServerIds())
                            .disabledSkillIds(request.getDisabledSkillIds())
                            .streamWriter(new FluxBridgeChatStreamWriter(sink, state))
                            .requestUser(requestUser)
                            .openId(request.getOpenId())
                            .sid(sid)
                            .build());
                } catch (Exception e) {
                    log.error("Chat stream error", e);
                    emitError(sink, state, e);
                    complete(sink);
                }
            });

            sink.onCancel(() -> log.debug("Chat stream cancelled"));
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * 同步对话 — 阻塞等待完整响应
     */
    public OpenApiChatSyncResponse chatSync(OpenApiChatRequest request) {
        long start = System.currentTimeMillis();
        CollectingChatStreamWriter collector = new CollectingChatStreamWriter();
        UserPO requestUser = resolveRequestUser(request.getOpenId());

        String sid = StringUtils.hasText(request.getSid())
                ? request.getSid()
                : UUID.randomUUID().toString();

        agentChatService.chat(AgentChatCommand.builder()
                .agentId(request.getAgentId())
                .conversationId(request.getConversationId())
                .content(request.getContent())
                .disabledMcpServerIds(request.getDisabledMcpServerIds())
                .disabledSkillIds(request.getDisabledSkillIds())
                .streamWriter(collector)
                .requestUser(requestUser)
                .openId(request.getOpenId())
                .sid(sid)
                .build());

        try {
            String fullText = collector.awaitAndGetFullText(SYNC_CHAT_TIMEOUT_MS);
            return OpenApiChatSyncResponse.builder()
                    .conversationId(request.getConversationId())
                    .content(fullText)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SnailAiException("对话被中断", e);
        } catch (TimeoutException e) {
            throw new SnailAiException("对话响应超时", e);
        }
    }

    private UserPO resolveRequestUser(String openId) {
        String appId = OpenApiSessionUtils.current().getAppId();
        return openApiUserResolver.resolvePlatformUser(appId, openId);
    }

    private void emitError(FluxSink<OpenApiChatStreamEvent> sink, ChatStreamState state, Throwable error) {
        String message = error != null && error.getMessage() != null ? error.getMessage() : "Unknown error";
        emit(sink, OpenApiChatStreamEvent.error(message));
        state.doneSent = true;
    }

    private void emitDoneIfNeeded(FluxSink<OpenApiChatStreamEvent> sink, ChatStreamState state) {
        if (state.doneSent) {
            return;
        }
        state.doneSent = true;
        emit(sink, OpenApiChatStreamEvent.done(state.conversationId, state.buffer.toString()));
    }

    private void emit(FluxSink<OpenApiChatStreamEvent> sink, OpenApiChatStreamEvent event) {
        if (!sink.isCancelled()) {
            sink.next(event);
        }
    }

    private void complete(FluxSink<OpenApiChatStreamEvent> sink) {
        if (!sink.isCancelled()) {
            sink.complete();
        }
    }

    private static class ChatStreamState {
        private final String conversationId;
        private final StringBuilder buffer = new StringBuilder();
        private boolean doneSent;

        private ChatStreamState(String conversationId) {
            this.conversationId = conversationId;
        }
    }

    /**
     * 桥接 ChatStreamWriter → FluxSink，将回调式推送转为响应式流
     */
    private class FluxBridgeChatStreamWriter implements ChatStreamWriter {

        private final FluxSink<OpenApiChatStreamEvent> sink;
        private final ChatStreamState state;

        private FluxBridgeChatStreamWriter(FluxSink<OpenApiChatStreamEvent> sink, ChatStreamState state) {
            this.sink = sink;
            this.state = state;
        }

        @Override
        public void send(String data) {
            ChatStreamResponse response = parseResponse(data);
            if (response == null) {
                return;
            }
            switch (response.getType()) {
                case ChatStreamResponse.TYPE_TEXT -> {
                    if (response.getContent() != null) {
                        state.buffer.append(response.getContent());
                        emit(sink, OpenApiChatStreamEvent.text(data));
                    }
                }
                case ChatStreamResponse.TYPE_THINKING ->
                        emit(sink, OpenApiChatStreamEvent.thinking(data));
                default ->
                        emit(sink, OpenApiChatStreamEvent.of(response.getType(), data));
            }
        }

        @Override
        public void complete() {
            emitDoneIfNeeded(sink, state);
            OpenApiChatService.this.complete(sink);
        }

        @Override
        public void completeWithError(Throwable ex) {
            emitError(sink, state, ex);
            OpenApiChatService.this.complete(sink);
        }

        private ChatStreamResponse parseResponse(String data) {
            try {
                return JsonUtil.parseObject(data.trim(), ChatStreamResponse.class);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
