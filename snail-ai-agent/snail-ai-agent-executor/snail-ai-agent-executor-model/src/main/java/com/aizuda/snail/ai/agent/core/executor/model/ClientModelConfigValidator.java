package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

/**
 * 客户端模型配置校验器。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class ClientModelConfigValidator {

    /**
     * 校验模型配置必填项（apiEndpoint/apiKey/modelKey），不合法时抛出异常。
     *
     * @param modelConfig 待校验的对话模型配置
     */
    public void validate(ChatDispatchRequest.ModelConfig modelConfig) {
        if (modelConfig == null) {
            throw new IllegalArgumentException("Model config is required");
        }
        require(modelConfig.getApiEndpoint(), "apiEndpoint");
        require(modelConfig.getApiKey(), "apiKey");
        require(modelConfig.getModelKey(), "modelKey");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Model config " + field + " is required");
        }
    }
}
