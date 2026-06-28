package com.aizuda.snail.ai.model.chat;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * ChatModelSpec
 *
 * @author opensnail
 * @since 2026-06-14
 */
public record ChatModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
