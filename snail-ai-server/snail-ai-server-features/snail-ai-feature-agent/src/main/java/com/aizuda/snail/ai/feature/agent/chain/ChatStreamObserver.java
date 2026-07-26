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
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
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
                default -> { /* 未知类型分片直接忽略，保证协议向前兼容 */ }
            }
        } catch (Exception e) {
            // 单个分片转发失败不中断整个流，仅记录并继续处理后续分片
            log.warn("流式分片转发失败, conversationId={}", context.getConversationId(), e);
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("gRPC 流异常中断, conversationId={}", context.getConversationId(), t);
        context.getStreamWriter().send("[ERROR] Client disconnected: " + t.getMessage());
        context.getStreamWriter().complete();
    }

    @Override
    public void onCompleted() {
        log.debug("gRPC 流正常结束, conversationId={}", context.getConversationId());
        context.getStreamWriter().complete();
    }

    /** 转发正文分片：重新序列化为标准 text 帧，按行分隔写给前端 */
    private void handleText(ChatStreamResponse data) {
        String json = JsonUtil.toJsonString(ChatStreamResponse.text(data.getSid(), data.getContent()));
        context.getStreamWriter().send(json + "\n");
    }

    /** 转发思考过程分片：空内容不下发，避免前端渲染空思考块 */
    private void handleThinking(ChatStreamResponse data) {
        String text = data.getContent();
        if (text == null || text.isEmpty()) {
            return;
        }
        String json = JsonUtil.toJsonString(ChatStreamResponse.thinking(data.getSid(), text));
        context.getStreamWriter().send(json + "\n");
    }

    /** 处理完成帧：记录用量指标，并异步持久化完整回复、思考过程与短期记忆 */
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

    /** 处理错误帧：透传客户端上报的错误给前端，流本身不在此终止（由 onCompleted/onError 收尾） */
    private void handleError(ChatStreamResponse data) {
        log.error("客户端执行出错, conversationId={}, error={}", context.getConversationId(), data.getErrorMessage());
        context.getStreamWriter().send("[ERROR] " + data.getErrorMessage());
    }

}
