package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FusionStrategy {

    RRF("RRF"),
    WEIGHTED_SUM("WEIGHTED_SUM");

    private final String strategy;
}
