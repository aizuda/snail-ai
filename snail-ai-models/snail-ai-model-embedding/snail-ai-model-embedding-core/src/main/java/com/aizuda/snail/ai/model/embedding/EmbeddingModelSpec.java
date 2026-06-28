package com.aizuda.snail.ai.model.embedding;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

public record EmbeddingModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
