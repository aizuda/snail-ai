package com.aizuda.snail.ai.vector.storage.vector;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 向量库构建配置：连接配置、嵌入模型与向量维度。
 *
 * @author opensnail
 * @since 2026-04-02
 */
@Data
@Builder
public class VectorStoreConfigDTO {

    /**
     * 向量库连接配置（来自存储实例 config JSON）
     */
    private String config;

    /**
     * 嵌入模型（用于向量化）
     */
    private EmbeddingModel embeddingModel;

    /**
     * 向量维度
     */
    private Integer dimensions;

}
