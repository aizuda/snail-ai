package com.aizuda.snail.ai.model.model.embedding;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.model.Model;
import com.aizuda.snail.ai.common.model.embedding.SnailEmbeddingResponse;

import java.util.List;

/**
 * 向量模型接口
 * author: opensnail
 * date: 2026-03-04
 */
public interface SnailEmbeddingModel extends Model {

    /**
     * 单个文本向量化
     */
    SnailEmbeddingResponse embed(EmbeddingModelDTO dto) throws ModelCallException;

    /**
     * 批量文本向量化
     */
    SnailEmbeddingResponse embedBatch(EmbeddingBatchModelDTO dto) throws ModelCallException;

    /**
     * 单个文本向量化请求DTO
     *
     * @param text 待向量化的文本
     * @param dimensions 向量维度（可选）
     */
    record EmbeddingModelDTO(String text, Integer dimensions) {
    }

    /**
     * 批量文本向量化请求DTO
     *
     * @param texts 待向量化的文本列表
     * @param dimensions 向量维度（可选）
     */
    record EmbeddingBatchModelDTO(List<String> texts, Integer dimensions) {
    }

    /**
     * 暴露 Spring AI 的嵌入模型，供向量库等与 Spring AI VectorStore 对接。
     * 默认实现不支持；由 {@link DefaultSnailEmbeddingModel} 提供具体实现。
     */
    default org.springframework.ai.embedding.EmbeddingModel toSpringAiEmbeddingModel() {
        throw new UnsupportedOperationException(
                "toSpringAiEmbeddingModel() is only supported by DefaultSnailEmbeddingModel");
    }
}
