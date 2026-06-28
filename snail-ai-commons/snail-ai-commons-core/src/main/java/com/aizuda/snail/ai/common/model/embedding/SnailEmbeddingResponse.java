package com.aizuda.snail.ai.common.model.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SnailEmbeddingResponse
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SnailEmbeddingResponse {

    private String model;

    private Integer dimensions;

    private List<EmbeddingVector> vectors;

    private Usage usage;

    private Long costTimeMs;

    public float[] firstVector() {
        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalStateException("No vectors in response");
        }
        return vectors.get(0).getVector();
    }
}
