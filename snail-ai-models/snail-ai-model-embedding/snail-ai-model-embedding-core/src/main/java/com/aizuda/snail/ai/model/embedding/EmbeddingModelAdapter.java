package com.aizuda.snail.ai.model.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

/**
 * EmbeddingModelAdapter
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface EmbeddingModelAdapter {

    String adapterKey();

    EmbeddingModel create(EmbeddingModelSpec spec);
}
