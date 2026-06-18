package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.persist.ChatResultPersistCommand;
import com.aizuda.snail.ai.feature.agent.persist.ChatResultPersistService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 流式响应观察者：桥接 Agent gRPC 流到前端 HTTP Emitter，并在 completion 时持久化结果。
 */
@Slf4j
public class ChatStreamObserver implements StreamObserver<GrpcSnailAiResult> {

    private final AgentChatContext context;
    private final ChatResultPersistService persistService;
    private final int shortTermWindow;

    public ChatStreamObserver(AgentChatContext context,
                              ChatResultPersistService persistService,
                              int shortTermWindow) {
        this.context = context;
        this.persistService = persistService;
        this.shortTermWindow = shortTermWindow;
    }

    @Override
    public void onNext(GrpcSnailAiResult result) {
        try {
            ChatStreamResponse data = JsonUtil.parseObject(result.getData(), ChatStreamResponse.class);
            if (data.getType() == null) {
                return;
            }

            switch (data.getType()) {
                case ChatStreamResponse.TYPE_TEXT -> handleText(data);
                case ChatStreamResponse.TYPE_THINKING -> handleThinking(data);
                case ChatStreamResponse.TYPE_COMPLETION -> handleCompletion(data);
                case ChatStreamResponse.TYPE_ERROR -> handleError(data);
                default -> { /* ignore unknown */ }
            }
        } catch (Exception e) {
            log.warn("Failed to proxy chunk to emitter", e);
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("gRPC stream error from client", t);
        context.getStreamWriter().send("[ERROR] Client disconnected: " + t.getMessage());
        context.getStreamWriter().complete();
    }

    @Override
    public void onCompleted() {
        context.getStreamWriter().complete();
    }

    private void handleText(ChatStreamResponse data) {
        String json = JsonUtil.toJsonString(ChatStreamResponse.text(data.getSid(), data.getContent()));
        context.getStreamWriter().send(json + "\n");
    }

    private void handleThinking(ChatStreamResponse data) {
        String text = data.getContent();
        if (text == null || text.isEmpty()) {
            return;
        }
        String json = JsonUtil.toJsonString(ChatStreamResponse.thinking(data.getSid(), text));
        context.getStreamWriter().send(json + "\n");
    }

    private void handleCompletion(ChatStreamResponse data) {
        String fullText = data.getFullText();
        String fullThinking = data.getFullThinking();
        log.info("Chat completed from client: conversationId={}, durationMs={}, inputTokens={}, outputTokens={}, cacheTokens={}",
                context.getConversationId(), data.getDurationMs(),
                data.getPromptTokens(), data.getCompletionTokens(), data.getCacheTokens());

        persistService.persistAsync(ChatResultPersistCommand.builder()
                .agentId(context.getAgent().getId())
                .userId(context.getUser().getId())
                .conversationId(context.getConversationId())
                .fullText(fullText)
                .thinkingText(fullThinking)
                .inputTokens(data.getPromptTokens() != null ? data.getPromptTokens() : 0)
                .outputTokens(data.getCompletionTokens() != null ? data.getCompletionTokens() : 0)
                .cacheTokens(data.getCacheTokens() != null ? data.getCacheTokens() : 0)
                .memoryEnabled(context.getAgent().getMemoryEnabled())
                .agentModelId(context.getModelId())
                .shortTermWindow(shortTermWindow)
                .build());
    }

    private void handleError(ChatStreamResponse data) {
        log.error("Client error: {}", data.getErrorMessage());
        context.getStreamWriter().send("[ERROR] " + data.getErrorMessage());
    }

}
