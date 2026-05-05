package com.aizuda.snail.ai.vector.storage.vector;

import com.aizuda.snail.ai.model.model.embedding.SnailEmbeddingModel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

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
    private SnailEmbeddingModel embeddingModel;
    private Integer dimensions;

}
