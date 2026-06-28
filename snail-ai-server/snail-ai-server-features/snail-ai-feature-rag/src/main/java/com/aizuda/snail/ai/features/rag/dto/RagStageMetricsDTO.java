package com.aizuda.snail.ai.features.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索各阶段指标：嵌入 / 向量 / BM25 / 融合 / 重排耗时与命中数。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagStageMetricsDTO {
    private long embeddingMs;
    private long vectorSearchMs;
    private long bm25SearchMs;
    private long fusionMs;
    private long rerankMs;
    private long totalMs;

    private int vectorHitCount;
    private int bm25HitCount;
    private int finalCount;
}
