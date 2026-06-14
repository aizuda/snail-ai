package com.aizuda.snail.ai.model.builder.rerank;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.model.adapter.server.ServerModelFacade;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rerank 客户端动态构建器。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RerankClientBuilder {

    private final ServerModelFacade serverModelFacade;

    public RerankApiClient buildRerankClient(String decryptedApiKey, ModelConfigInfoDTO config) {
        try {
            RerankApiClient client = serverModelFacade.buildRerankClient(config, decryptedApiKey);
            log.info("Successfully built RerankApiClient for config: {} (model: {})",
                    config.getId(), config.getModelKey());
            return client;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build RerankApiClient for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "RerankClient 构建失败: " + e.getMessage(),
                    e);
        }
    }
}
