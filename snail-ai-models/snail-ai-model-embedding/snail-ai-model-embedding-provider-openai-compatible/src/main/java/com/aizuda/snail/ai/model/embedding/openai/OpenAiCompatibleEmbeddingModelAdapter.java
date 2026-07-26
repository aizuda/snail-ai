package com.aizuda.snail.ai.model.embedding.openai;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelAdapter;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.Locale;

/**
 * OpenAI 兼容向量化模型适配器，基于 Spring AI {@code OpenAiEmbeddingModel} 按规格与扩展参数构建向量化模型。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
public class OpenAiCompatibleEmbeddingModelAdapter implements EmbeddingModelAdapter, Ordered {

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    /** 构建失败时的默认重试次数 */
    private static final int DEFAULT_MAX_RETRIES = 2;

    @Override
    public String adapterKey() {
        return ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER;
    }

    @Override
    public EmbeddingModel create(EmbeddingModelSpec spec) {
        log.debug("Creating OpenAI-compatible EmbeddingModel, provider={}, model={}",
                spec.providerKey(), spec.modelKey());
        return OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(buildOptions(spec))
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 由规格与扩展配置组装向量化选项（地址、密钥、超时、重试、维度与编码格式）
     *
     * @param spec 向量化模型构建规格
     * @return 向量化选项
     */
    private OpenAiEmbeddingOptions buildOptions(EmbeddingModelSpec spec) {
        ConfigExtAttrsDTO config = spec.configJson();
        OpenAiEmbeddingOptions.Builder builder = OpenAiEmbeddingOptions.builder()
                .baseUrl(spec.baseUrl())
                .apiKey(spec.apiKey())
                .model(spec.modelKey())
                .timeout(Duration.ofMillis(readTimeoutMs(config)))
                .maxRetries(readMaxRetries(config));
        if (config == null) {
            return builder.build();
        }
        if (config.getEmbeddingDimension() != null) {
            builder.dimensions(config.getEmbeddingDimension());
        }
        OpenAiEmbeddingOptions.EncodingFormat encodingFormat = parseEncodingFormat(config.getEncodingFormat());
        if (encodingFormat != null) {
            builder.encodingFormat(encodingFormat);
        }
        return builder.build();
    }

    /**
     * 解析编码格式配置，非法值降级为 null（使用服务端默认格式）而不阻断模型构建
     *
     * @param encodingFormat 配置的编码格式字符串（float / base64）
     * @return 对应枚举；为空或无法识别时返回 null
     */
    private OpenAiEmbeddingOptions.EncodingFormat parseEncodingFormat(String encodingFormat) {
        if (encodingFormat == null || encodingFormat.isBlank()) {
            return null;
        }
        try {
            return OpenAiEmbeddingOptions.EncodingFormat.valueOf(encodingFormat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized embedding encodingFormat '{}', fallback to provider default", encodingFormat);
            return null;
        }
    }

    private long readTimeoutMs(ConfigExtAttrsDTO config) {
        Long timeoutMs = config != null ? config.getTimeoutMs() : null;
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    private int readMaxRetries(ConfigExtAttrsDTO config) {
        Integer maxRetries = config != null ? config.getMaxRetries() : null;
        return maxRetries != null && maxRetries >= 0 ? maxRetries : DEFAULT_MAX_RETRIES;
    }
}
