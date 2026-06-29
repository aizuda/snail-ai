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
    /**
     * 向量化（embedding）耗时（毫秒）
     */
    private long embeddingMs;

    /**
     * 向量检索耗时（毫秒）
     */
    private long vectorSearchMs;

    /**
     * BM25 检索耗时（毫秒）
     */
    private long bm25SearchMs;

    /**
     * 结果融合耗时（毫秒）
     */
    private long fusionMs;

    /**
     * 重排耗时（毫秒）
     */
    private long rerankMs;

    /**
     * 总耗时（毫秒）
     */
    private long totalMs;

    /**
     * 向量检索命中数
     */
    private int vectorHitCount;

    /**
     * BM25 检索命中数
     */
    private int bm25HitCount;

    /**
     * 最终返回结果数
     */
    private int finalCount;
}
