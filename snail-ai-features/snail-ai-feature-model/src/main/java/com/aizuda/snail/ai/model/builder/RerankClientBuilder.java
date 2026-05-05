package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rerank 客户端动态构建器
 * 参照 EmbeddingClientBuilder 设计
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RerankClientBuilder {

    private final List<RerankModelFactory> rerankModelFactories;

    /**
     * 构建 RerankApiClient
     *
     * @param decryptedApiKey 解密后的 API Key
     * @param config          模型配置信息
     * @return RerankApiClient 实例
     */
    public RerankApiClient buildRerankClient(String decryptedApiKey, ModelConfigInfoDTO config)
            throws ModelCallException {
        try {
            RerankModelFactory factory = getRerankModelFactory(config.getProviderKey());

            RerankApiClient client = factory.createRerankClient(
                    config.getProviderKey(),
                    config.getApiEndpoint(),
                    decryptedApiKey,
                    config.getModelKey(),
                    config.getConfigJson()
            );

            log.info("Successfully built RerankApiClient for config: {} (model: {})",
                    config.getId(), config.getModelKey());
            return client;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build RerankApiClient for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "RerankClient 构建失败: " + e.getMessage(), e);
        }
    }

    private RerankModelFactory getRerankModelFactory(String providerKey) throws ModelCallException {
        if (rerankModelFactories == null || rerankModelFactories.isEmpty()) {
            throw new ModelCallException(
                    ModelCallException.ErrorCode.PROVIDER_NOT_SUPPORTED,
                    "未注册任何 Rerank 工厂");
        }
        String key = providerKey != null ? providerKey.trim() : "";
        RerankModelFactory factory = rerankModelFactories.stream()
                .filter(f -> key.equalsIgnoreCase(f.getSupportedProvider()))
                .findFirst()
                .orElse(null);
        if (factory == null) {
            // 多数兼容 OpenAI 风格 /rerank HTTP 的厂商：回退到 openai 实现
            factory = rerankModelFactories.stream()
                    .filter(f -> "openai".equalsIgnoreCase(f.getSupportedProvider()))
                    .findFirst()
                    .orElse(null);
            if (factory != null) {
                log.warn("No RerankModelFactory for provider [{}], using openai-compatible HTTP client", providerKey);
            }
        }
        if (factory == null) {
            throw new ModelCallException(
                    ModelCallException.ErrorCode.PROVIDER_NOT_SUPPORTED,
                    "不支持的提供商: " + providerKey);
        }
        return factory;
    }
}
