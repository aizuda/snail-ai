package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 混合检索融合策略：RRF 与加权和。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum FusionStrategy {

    /** 倒数排名融合（Reciprocal Rank Fusion） */
    RRF("RRF"),
    /** 加权求和融合 */
    WEIGHTED_SUM("WEIGHTED_SUM");

    private final String strategy;
}
