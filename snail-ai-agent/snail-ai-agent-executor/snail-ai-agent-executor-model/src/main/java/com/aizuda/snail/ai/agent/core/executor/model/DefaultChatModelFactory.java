package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.chat.ChatModelRuntime;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 默认 ChatModel 工厂。
 */
public class DefaultChatModelFactory implements ChatModelFactory {

    private final ClientChatModelSpecFactory clientChatModelSpecFactory;
    private final ChatModelRuntime chatModelRuntime;

    public DefaultChatModelFactory(ClientChatModelSpecFactory clientChatModelSpecFactory,
                                   ChatModelRuntime chatModelRuntime) {
        this.clientChatModelSpecFactory = clientChatModelSpecFactory;
        this.chatModelRuntime = chatModelRuntime;
    }

    @Override
    public ChatModel build(ChatDispatchRequest.ModelConfig modelConfig) {
        return chatModelRuntime.build(clientChatModelSpecFactory.create(new ClientChatModelInput(modelConfig)));
    }
}
