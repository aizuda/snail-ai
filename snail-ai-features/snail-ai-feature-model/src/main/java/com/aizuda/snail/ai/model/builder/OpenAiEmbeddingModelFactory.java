package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * OpenAI SnailEmbeddingModel 工厂实现
 * 支持 OpenAI 及兼容接口（火山云、通义千问等）
 */
@Slf4j
@Component
public class OpenAiEmbeddingModelFactory implements EmbeddingModelFactory {

    @Override
    public String getSupportedProvider() {
        return "openai";
    }

    @Override
    public EmbeddingModel createEmbeddingModel(String providerKey, String baseUrl, String apiKey,
                                               String modelKey, ConfigExtAttrsDTO configExtAttrsDTO) throws Exception {
        try {
            // 1. 验证配置
            if (!isConfigValid(baseUrl, apiKey)) {
                throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                        "baseUrl 和 apiKey 不能为空");
            }

            log.debug("Creating OpenAiEmbeddingModel for provider: {}, model: {}, baseUrl: {}",
                    providerKey, modelKey, baseUrl);

            if (configExtAttrsDTO != null && configExtAttrsDTO.getEmbeddingsPath() != null
                    && !configExtAttrsDTO.getEmbeddingsPath().isBlank()
                    && !"/v1/embeddings".equals(configExtAttrsDTO.getEmbeddingsPath())) {
                log.warn("Spring AI 2.0.0-M7 OpenAI SDK no longer supports per-model embeddingsPath directly: {}",
                        configExtAttrsDTO.getEmbeddingsPath());
            }

            OpenAiEmbeddingOptions embeddingOptions = parseAndBuildOptions(baseUrl, apiKey, configExtAttrsDTO, modelKey);

            OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(MetadataMode.EMBED, embeddingOptions);

            log.info("Successfully created OpenAiEmbeddingModel for model: {}", modelKey);
            return embeddingModel;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create OpenAiEmbeddingModel for model: {}", modelKey, e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "无法创建EmbeddingModel: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 解析配置JSON并构建OpenAiEmbeddingOptions
     *
     * @param configJson 配置JSON对象，格式如:
     *                   {
     *                   "model": "text-embedding-3-large",
     *                   "dimensions": 1536,
     *                   "encodingFormat": "float"
     *                   }
     * @param modelKey  模型标识符（默认值）
     */
    private OpenAiEmbeddingOptions parseAndBuildOptions(String baseUrl, String apiKey,
                                                        ConfigExtAttrsDTO configJson, String modelKey) {
        try {
            OpenAiEmbeddingOptions.Builder builder = OpenAiEmbeddingOptions.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .model(modelKey);

            if (configJson != null) {
                Long timeoutMs = configJson.getTimeoutMs();
                long readTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 60_000L;
                builder.timeout(Duration.ofMillis(readTimeoutMs));
                builder.dimensions(configJson.getEmbeddingDimension());
                OpenAiEmbeddingOptions.EncodingFormat encodingFormat = parseEncodingFormat(configJson.getEncodingFormat());
                if (encodingFormat != null) {
                    builder.encodingFormat(encodingFormat);
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse configJson: {}, using default options", configJson, e);
            return OpenAiEmbeddingOptions.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .model(modelKey)
                    .build();
        }
    }

    private OpenAiEmbeddingOptions.EncodingFormat parseEncodingFormat(String encodingFormat) {
        if (encodingFormat == null || encodingFormat.isBlank()) {
            return null;
        }
        try {
            return OpenAiEmbeddingOptions.EncodingFormat.valueOf(encodingFormat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported embedding encodingFormat: {}", encodingFormat);
            return null;
        }
    }
}
