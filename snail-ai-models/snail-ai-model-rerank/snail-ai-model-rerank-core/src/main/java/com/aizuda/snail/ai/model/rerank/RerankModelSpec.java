package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * RerankModelSpec
 *
 * @author opensnail
 * @since 2026-06-14
 */
public record RerankModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
