package com.aizuda.snail.ai.vector.storage.vector;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 向量库构建配置：连接配置、嵌入模型与向量维度。
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
