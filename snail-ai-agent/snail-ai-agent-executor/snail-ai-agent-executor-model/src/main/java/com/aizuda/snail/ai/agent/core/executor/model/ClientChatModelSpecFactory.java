package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;

/**
 * 客户端 dispatch 模型配置到 ChatModelSpec 的适配器。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class ClientChatModelSpecFactory {

    private final ClientModelConfigValidator validator;

    public ClientChatModelSpecFactory(ClientModelConfigValidator validator) {
        this.validator = validator;
    }

    /**
     * 将客户端模型输入校验并适配为 {@link ChatModelSpec}。
     *
     * @param input 客户端 Chat 模型输入
     * @return 适配后的 ChatModelSpec
     */
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
