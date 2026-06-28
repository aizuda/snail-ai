package com.aizuda.snail.ai.model.chat;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

public record ChatModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
