package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FusionStrategy
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum FusionStrategy {

    RRF("RRF"),
    WEIGHTED_SUM("WEIGHTED_SUM");

    private final String strategy;
}
