package com.aizuda.snail.ai.common.model;

import java.util.List;
import lombok.Data;

/** (复制自 snail-job-ai-executor) */
@Data
public class EmbeddingResponseDTO {

    private Integer totalTokens;
    private List<EmbeddingItem> embeddings;

    @Data
    public static class EmbeddingItem {
        private float[] embedding;
        private Integer index;
    }
}
