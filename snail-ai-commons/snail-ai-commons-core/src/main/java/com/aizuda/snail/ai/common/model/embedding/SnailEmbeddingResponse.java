package com.aizuda.snail.ai.common.model.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文本向量化响应，封装模型信息、向量列表、Token 用量统计与调用耗时。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SnailEmbeddingResponse {

    /**
     * 实际使用的模型名
     */
    private String model;

    /**
     * 向量维度
     */
    private Integer dimensions;

    /**
     * 向量结果列表
     */
    private List<EmbeddingVector> vectors;

    /**
     * Token 用量统计
     */
    private Usage usage;

    /**
     * 本次调用耗时（毫秒）
     */
    private Long costTimeMs;

    public float[] firstVector() {
        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalStateException("No vectors in response");
        }
        return vectors.get(0).getVector();
    }
}
