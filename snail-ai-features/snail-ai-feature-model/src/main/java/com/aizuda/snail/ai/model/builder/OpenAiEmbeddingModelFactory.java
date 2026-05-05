package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

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

            String embeddingsPath = "/v1/embeddings";
            if (configExtAttrsDTO != null && configExtAttrsDTO.getEmbeddingsPath() != null
                    && !configExtAttrsDTO.getEmbeddingsPath().isEmpty()) {
                embeddingsPath = configExtAttrsDTO.getEmbeddingsPath();
            }

            Long timeoutMs = configExtAttrsDTO != null ? configExtAttrsDTO.getTimeoutMs() : null;
            long readTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 60_000L;
            long connectTimeoutMs = Math.min(readTimeoutMs, 10_000L);

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
            requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

            // 2. 创建 OpenAiApi
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .embeddingsPath(embeddingsPath)
                    .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                    .build();

            // 3. 解析配置JSON，构建 OpenAiEmbeddingOptions
            OpenAiEmbeddingOptions embeddingOptions = parseAndBuildOptions(configExtAttrsDTO, modelKey);

            // 4. 创建 SnailEmbeddingModel
            OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                    openAiApi,
                    MetadataMode.EMBED,
                    embeddingOptions,
                    RetryUtils.DEFAULT_RETRY_TEMPLATE
            );

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
     * @param configJson 配置JSON字符串，格式如:
     *                   {
     *                   "model": "text-embedding-3-large",
     *                   "dimensions": 1536,
     *                   "encodingFormat": "float"
     *                   }
     * @param modelKey  模型标识符（默认值）
     */
    private OpenAiEmbeddingOptions parseAndBuildOptions(ConfigExtAttrsDTO configJson, String modelKey) {
        if (configJson == null) {
            return null;
        }
        try {
            OpenAiEmbeddingOptions.Builder builder = OpenAiEmbeddingOptions.builder();

            // 设置默认模型名称
            builder.model(modelKey);
            builder.dimensions(configJson.getEmbeddingDimension());
            builder.encodingFormat(configJson.getEncodingFormat());

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse configJson: {}, using default options", configJson, e);
            return OpenAiEmbeddingOptions.builder()
                    .model(modelKey)
                    .build();
        }
    }
}
