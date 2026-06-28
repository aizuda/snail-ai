package com.aizuda.snail.ai.agent.core.executor;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端 Chat 执行请求上下文。
 */
@Data
@Builder
public class ClientChatExecutionRequest {

    private ChatDispatchRequest dispatchRequest;
    private List<ToolCallback> tools;
    private Consumer<String> messageDeltaConsumer;
    private Consumer<String> thinkingDeltaConsumer;
    private Consumer<ClientChatExecutor.ChatCompletionResult> completionConsumer;
    private Consumer<Throwable> errorConsumer;

    public void acceptCompletion(ClientChatExecutor.ChatCompletionResult completion) {
        if (completionConsumer != null) {
            completionConsumer.accept(completion);
        }
    }

    public void acceptError(Throwable error) {
        if (errorConsumer != null) {
            errorConsumer.accept(error);
        }
    }
}
