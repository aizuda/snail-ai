package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

public record RerankModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
