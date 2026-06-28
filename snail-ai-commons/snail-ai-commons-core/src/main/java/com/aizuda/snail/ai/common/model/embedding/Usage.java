package com.aizuda.snail.ai.common.model.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Usage {

    private Integer promptTokens;

    private Integer totalTokens;
}
