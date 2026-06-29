package com.aizuda.snail.ai.common.model.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条文本的向量化结果，包含批内序号、原始文本与向量值。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbeddingVector {

    /**
     * 在批量请求中的序号
     */
    private int index;

    /**
     * 原始输入文本
     */
    private String input;

    /**
     * 向量值（嵌入）
     */
    private float[] vector;
}
