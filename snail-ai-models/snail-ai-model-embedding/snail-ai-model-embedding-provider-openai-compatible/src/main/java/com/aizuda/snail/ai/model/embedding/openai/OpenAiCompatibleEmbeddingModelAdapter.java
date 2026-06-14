package com.aizuda.snail.ai.model.embedding.openai;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelAdapter;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelSpec;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.Locale;

public class OpenAiCompatibleEmbeddingModelAdapter implements EmbeddingModelAdapter, Ordered {

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    @Override
    public String adapterKey() {
        return ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER;
    }

    @Override
    public EmbeddingModel create(EmbeddingModelSpec spec) {
        return new OpenAiEmbeddingModel(MetadataMode.EMBED, buildOptions(spec));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private OpenAiEmbeddingOptions buildOptions(EmbeddingModelSpec spec) {
        ConfigExtAttrsDTO config = spec.configJson();
        OpenAiEmbeddingOptions.Builder builder = OpenAiEmbeddingOptions.builder()
                .baseUrl(spec.baseUrl())
                .apiKey(spec.apiKey())
                .model(spec.modelKey())
                .timeout(Duration.ofMillis(readTimeoutMs(config)));
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

    private OpenAiEmbeddingOptions.EncodingFormat parseEncodingFormat(String encodingFormat) {
        if (encodingFormat == null || encodingFormat.isBlank()) {
            return null;
        }
        try {
            return OpenAiEmbeddingOptions.EncodingFormat.valueOf(encodingFormat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private long readTimeoutMs(ConfigExtAttrsDTO config) {
        Long timeoutMs = config != null ? config.getTimeoutMs() : null;
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
}
