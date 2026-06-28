package com.aizuda.snail.ai.model.embedding;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * EmbeddingModelSpec
 *
 * @author opensnail
 * @since 2026-06-14
 */
public record EmbeddingModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
