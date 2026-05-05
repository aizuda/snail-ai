package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SnailEmbeddingModel 动态构建器
 * 根据模型配置在运行时动态构建 SnailEmbeddingModel 实例
 * 支持多种提供商（OpenAI、Claude 等）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClientBuilder {

    private final List<EmbeddingModelFactory> embeddingModelFactories;
    /**
     * 获取或构建 SnailEmbeddingModel（带缓存）
     *
     * @param decryptedApiKey 解密后的 API Key
     * @param config          模型配置信息
     * @return SnailEmbeddingModel 实例
     * @throws ModelCallException 如果构建失败
     */
    public EmbeddingModel getOrBuildEmbeddingModel(String decryptedApiKey, ModelConfigInfoDTO config)
            throws ModelCallException {

        try {
            // 2. 缓存失效，动态构建
            log.debug("Building SnailEmbeddingModel for config: {}, provider: {}", config.getId(), config.getProviderKey());
            EmbeddingModel embeddingModel = buildEmbeddingModel(config, decryptedApiKey);

            log.debug("SnailEmbeddingModel cached for config: {}", config.getId());

            return embeddingModel;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build SnailEmbeddingModel for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "SnailEmbeddingModel 构建失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 根据模型配置动态构建 SnailEmbeddingModel
     *
     * @param config          模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return SnailEmbeddingModel 实例
     * @throws ModelCallException 如果构建失败
     */
    public EmbeddingModel buildEmbeddingModel(ModelConfigInfoDTO config, String decryptedApiKey)
            throws ModelCallException {

        try {
            // 1. 获取对应提供商的 SnailEmbeddingModel 工厂
            EmbeddingModelFactory factory = getEmbeddingModelFactory(config.getProviderKey());

            // 2. 通过工厂创建 SnailEmbeddingModel
            EmbeddingModel embeddingModel = factory.createEmbeddingModel(
                    config.getProviderKey(),
                    config.getApiEndpoint(),
                    decryptedApiKey,
                    config.getModelKey(),
                    config.getConfigJson()
            );

            log.info("Successfully built SnailEmbeddingModel for config: {} (model: {})", config.getId(), config.getModelKey());

            return embeddingModel;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build SnailEmbeddingModel for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "SnailEmbeddingModel 构建失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 获取对应提供商的 SnailEmbeddingModel 工厂
     *
     * @param providerKey 提供商标识（如 "openai"）
     * @return SnailEmbeddingModel 工厂
     * @throws ModelCallException 如果提供商不支持
     */
    private EmbeddingModelFactory getEmbeddingModelFactory(String providerKey) throws ModelCallException {
        // todo 这里需要适配一下
        EmbeddingModelFactory factory = embeddingModelFactories.stream().findFirst().orElse(null);
        if (factory == null) {
            log.error("Unsupported provider for embedding: {}", providerKey);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.PROVIDER_NOT_SUPPORTED,
                    "不支持的提供商: " + providerKey
            );
        }
        return factory;
    }
}
