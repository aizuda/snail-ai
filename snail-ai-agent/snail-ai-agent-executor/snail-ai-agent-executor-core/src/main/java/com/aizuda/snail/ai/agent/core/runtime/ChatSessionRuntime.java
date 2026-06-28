package com.aizuda.snail.ai.agent.core.runtime;

import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;
import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutionRequest;
import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.agent.core.runtime.tool.ToolRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端一次 Chat 会话运行时，负责工具准备、活跃计数与资源清理。
 */
@Slf4j
@RequiredArgsConstructor
public class ChatSessionRuntime {

    private final ClientChatExecutor chatExecutor;
    private final ActiveChatCounter activeChatCounter;
    private final ToolRuntime toolRuntime;

    public void execute(ChatSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChatSessionRequest is required");
        }
        activeChatCounter.increment();
        ToolRuntime.ToolResolution resolution = null;
        try {
            resolution = toolRuntime.resolve(request.getDispatchRequest());
            ToolRuntime.ToolResolution currentResolution = resolution;
            chatExecutor.executeStream(ClientChatExecutionRequest.builder()
                    .dispatchRequest(request.getDispatchRequest())
                    .tools(currentResolution.getTools())
                    .messageDeltaConsumer(request.getTextConsumer())
                    .thinkingDeltaConsumer(request.getThinkingConsumer())
                    .completionConsumer(completion -> {
                        try {
                            request.acceptCompletion(completion);
                        } finally {
                            cleanup(currentResolution);
                        }
                    })
                    .errorConsumer(error -> {
                        try {
                            request.acceptError(error);
                        } finally {
                            cleanup(currentResolution);
                        }
                    })
                    .build());
        } catch (Exception e) {
            log.error("Failed to start chat session", e);
            cleanup(resolution);
            request.acceptError(e);
        }
    }

    private void cleanup(ToolRuntime.ToolResolution resolution) {
        activeChatCounter.decrement();
        if (resolution != null) {
            resolution.close();
        }
    }
}
