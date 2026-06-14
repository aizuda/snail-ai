package com.aizuda.snail.ai.vector.storage.vector;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2026-04-02
 */
@Data
@Builder
public class VectorStoreConfigDTO {

    private String config;
    private EmbeddingModel embeddingModel;
    private Integer dimensions;

}
