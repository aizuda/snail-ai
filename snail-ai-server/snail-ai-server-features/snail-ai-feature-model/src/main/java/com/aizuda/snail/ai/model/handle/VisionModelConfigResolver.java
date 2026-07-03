package com.aizuda.snail.ai.model.handle;

import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;

/**
 * Resolves the chat model configuration used for vision-capable fallback tasks.
 *
 * @author opensnail
 * @since 2026-07-03
 */
@FunctionalInterface
public interface VisionModelConfigResolver {

    ModelConfigInfoDTO getDefaultVisionChatModel();
}
