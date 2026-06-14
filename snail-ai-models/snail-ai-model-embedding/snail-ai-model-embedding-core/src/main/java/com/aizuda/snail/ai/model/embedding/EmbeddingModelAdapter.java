package com.aizuda.snail.ai.model.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

public interface EmbeddingModelAdapter {

    String adapterKey();

    EmbeddingModel create(EmbeddingModelSpec spec);
}
