package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.RerankApiClient;

/**
 * RerankModel 工厂接口 - 支持不同提供商的 Rerank 客户端创建
 */
public interface RerankModelFactory {

    /**
     * 获取该工厂支持的提供商标识
     */
    String getSupportedProvider();

    /**
     * 根据提供商和配置创建 Rerank API 客户端
     *
     * @param providerKey 提供商标识
     * @param baseUrl     API 基础 URL
     * @param apiKey      解密后的 API Key
     * @param modelKey    模型标识符
     * @param configJson  配置 JSON
     * @return Rerank API 客户端
     */
    RerankApiClient createRerankClient(String providerKey, String baseUrl, String apiKey,
                                       String modelKey, ConfigExtAttrsDTO configJson) throws Exception;

    default boolean isConfigValid(String baseUrl, String apiKey) {
        return baseUrl != null && !baseUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
    }
}
