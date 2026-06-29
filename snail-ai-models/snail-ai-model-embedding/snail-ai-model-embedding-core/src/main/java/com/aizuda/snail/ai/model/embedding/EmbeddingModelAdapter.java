package com.aizuda.snail.ai.model.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 向量化模型适配器接口，按规格创建对应供应商的 {@link EmbeddingModel}。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface EmbeddingModelAdapter {

    /**
     * 适配器唯一标识，用于与模型配置的 adapterKey 匹配
     *
     * @return 适配器键
     */
    String adapterKey();

    /**
     * 按规格创建向量化模型
     *
     * @param spec 向量化模型构建规格
     * @return 向量化模型实例
     */
    EmbeddingModel create(EmbeddingModelSpec spec);
}
