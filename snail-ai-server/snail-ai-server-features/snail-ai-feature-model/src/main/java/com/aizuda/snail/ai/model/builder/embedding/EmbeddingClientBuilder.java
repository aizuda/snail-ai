package com.aizuda.snail.ai.model.builder.embedding;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.adapter.server.ServerModelFacade;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * EmbeddingModel 动态构建器。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClientBuilder {

    private final ServerModelFacade serverModelFacade;

    /**
     * 获取向量化模型（当前每次新建，异常归一化为模型调用异常）
     *
     * @param decryptedApiKey 解密后的 API Key
     * @param config          模型配置信息
     * @return 向量化模型实例
     */
    public EmbeddingModel getOrBuildEmbeddingModel(String decryptedApiKey, ModelConfigInfoDTO config) {
        try {
            log.debug("Building EmbeddingModel for config: {}, provider: {}",
                    config.getId(), config.getProviderKey());
            EmbeddingModel embeddingModel = buildEmbeddingModel(config, decryptedApiKey);
            log.debug("EmbeddingModel built for config: {}", config.getId());
            return embeddingModel;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build EmbeddingModel for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.EMBEDDING_MODEL_BUILD_FAILED,
                    "EmbeddingModel 构建失败: " + e.getMessage(),
                    e);
        }
    }

    /**
     * 通过门面构建向量化模型
     *
     * @param config          模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return 向量化模型实例
     */
    public EmbeddingModel buildEmbeddingModel(ModelConfigInfoDTO config, String decryptedApiKey) {
        try {
            EmbeddingModel embeddingModel = serverModelFacade.buildEmbeddingModel(config, decryptedApiKey);
            log.info("Successfully built EmbeddingModel for config: {} (model: {})",
                    config.getId(), config.getModelKey());
            return embeddingModel;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build EmbeddingModel for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.EMBEDDING_MODEL_BUILD_FAILED,
                    "EmbeddingModel 构建失败: " + e.getMessage(),
                    e);
        }
    }
}
