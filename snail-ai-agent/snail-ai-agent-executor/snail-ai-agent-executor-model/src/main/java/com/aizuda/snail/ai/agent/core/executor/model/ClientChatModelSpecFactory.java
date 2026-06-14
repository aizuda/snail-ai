package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;

/**
 * 客户端 dispatch 模型配置到 ChatModelSpec 的适配器。
 */
public class ClientChatModelSpecFactory {

    private final ClientModelConfigValidator validator;

    public ClientChatModelSpecFactory(ClientModelConfigValidator validator) {
        this.validator = validator;
    }

    public ChatModelSpec create(ClientChatModelInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ClientChatModelInput is required");
        }
        ChatDispatchRequest.ModelConfig config = input.modelConfig();
        validator.validate(config);
        String adapterKey = ModelAdapterDefaults.resolve(
                config.getAdapterKey(), ModelAdapterDefaults.CHAT_MODEL_TYPE);
        return new ChatModelSpec(
                adapterKey,
                config.getProviderKey(),
                config.getApiEndpoint(),
                config.getApiKey(),
                config.getModelKey(),
                config.getConfigJson());
    }
}
