package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量检索结果，表示一条命中文档及其与查询的相似度得分。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorSearchResult {

    /**
     * 命中文档的唯一标识
     */
    private String id;

    /**
     * 命中文档的原始文本内容
     */
    private String content;

    /**
     * 与查询向量的相似度得分，越大越相关
     */
    private float score;

    /**
     * 命中文档的附加元数据
     */
    private Map<String, Object> metadata;
}
