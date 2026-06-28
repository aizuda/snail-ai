package com.aizuda.snail.ai.model.adapter.server;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelSpec;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.rerank.RerankModelSpec;
import org.springframework.stereotype.Component;

/**
 * 服务端模型配置到模型构建规格的适配器。
 */
@Component
public class ServerModelSpecFactory {

    public ChatModelSpec chatSpec(ModelConfigInfoDTO config, String decryptedApiKey) {
        requireConfig(config, ModelTypeEnum.CHAT);
        return new ChatModelSpec(
                resolveAdapterKey(config, ModelTypeEnum.CHAT),
                config.getProviderKey(),
                config.getApiEndpoint(),
                requireApiKey(decryptedApiKey),
                config.getModelKey(),
                config.getConfigJson());
    }

    public EmbeddingModelSpec embeddingSpec(ModelConfigInfoDTO config, String decryptedApiKey) {
        requireConfig(config, ModelTypeEnum.EMBEDDING);
        return new EmbeddingModelSpec(
                resolveAdapterKey(config, ModelTypeEnum.EMBEDDING),
                config.getProviderKey(),
                config.getApiEndpoint(),
                requireApiKey(decryptedApiKey),
                config.getModelKey(),
                config.getConfigJson());
    }

    public RerankModelSpec rerankSpec(ModelConfigInfoDTO config, String decryptedApiKey) {
        requireConfig(config, ModelTypeEnum.RERANKER);
        return new RerankModelSpec(
                resolveAdapterKey(config, ModelTypeEnum.RERANKER),
                config.getProviderKey(),
                config.getApiEndpoint(),
                requireApiKey(decryptedApiKey),
                config.getModelKey(),
                config.getConfigJson());
    }

    public String resolveAdapterKey(ModelConfigInfoDTO config, ModelTypeEnum modelType) {
        requireConfig(config, modelType);
        return ModelAdapterDefaults.resolve(config.getAdapterKey(), modelType.getValue());
    }

    private static void requireConfig(ModelConfigInfoDTO config, ModelTypeEnum expectedType) {
        if (config == null) {
            throw invalid("model config not found");
        }
        if (!expectedType.getValue().equalsIgnoreCase(config.getModelType())) {
            throw invalid("modelType must be " + expectedType.getValue());
        }
        if (StrUtil.isBlank(config.getProviderKey())) {
            throw invalid("providerKey is required");
        }
        if (StrUtil.isBlank(config.getApiEndpoint())) {
            throw invalid("apiEndpoint is required");
        }
        if (StrUtil.isBlank(config.getModelKey())) {
            throw invalid("modelKey is required");
        }
    }

    private static String requireApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            throw invalid("apiKey is required");
        }
        return apiKey;
    }

    private static ModelCallException invalid(String message) {
        return new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER, message);
    }
}
