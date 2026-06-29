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
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
public class ServerModelSpecFactory {

    /**
     * 将模型配置适配为聊天模型构建规格
     *
     * @param config          模型配置信息（modelType 须为 CHAT）
     * @param decryptedApiKey 解密后的 API Key
     * @return 聊天模型构建规格
     */
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

    /**
     * 将模型配置适配为向量化模型构建规格
     *
     * @param config          模型配置信息（modelType 须为 EMBEDDING）
     * @param decryptedApiKey 解密后的 API Key
     * @return 向量化模型构建规格
     */
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

    /**
     * 将模型配置适配为重排模型构建规格
     *
     * @param config          模型配置信息（modelType 须为 RERANKER）
     * @param decryptedApiKey 解密后的 API Key
     * @return 重排模型构建规格
     */
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

    /**
     * 解析模型配置使用的协议适配器标识，未显式配置时回退到类型默认值
     *
     * @param config    模型配置信息
     * @param modelType 模型类型
     * @return 协议适配器标识
     */
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
