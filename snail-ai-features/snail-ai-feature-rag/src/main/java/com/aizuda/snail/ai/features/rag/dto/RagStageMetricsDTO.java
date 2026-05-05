package com.aizuda.snail.ai.features.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
