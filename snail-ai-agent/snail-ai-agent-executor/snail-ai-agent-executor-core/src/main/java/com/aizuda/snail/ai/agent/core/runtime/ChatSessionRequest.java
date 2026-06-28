package com.aizuda.snail.ai.agent.core.runtime;

import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;

/** 客户端一次 Chat 会话的执行上下文。 */
@Getter
@Builder
public class ChatSessionRequest {

    private ChatDispatchRequest dispatchRequest;
    private Consumer<String> textConsumer;
    private Consumer<String> thinkingConsumer;
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
