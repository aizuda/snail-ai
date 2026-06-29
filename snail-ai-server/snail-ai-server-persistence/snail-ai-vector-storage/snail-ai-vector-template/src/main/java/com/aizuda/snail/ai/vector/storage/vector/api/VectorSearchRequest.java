package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量检索请求，封装目标索引、查询文本/向量、返回数量及过滤条件。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorSearchRequest {

    /**
     * 索引或集合名称；非空时优先于 filter 中的 ragId 推导。
     */
    private String indexName;

    /**
     * 查询文本；与 queryVector 二选一，提供文本时由嵌入模型现场向量化后检索
     */
    private String queryText;

    /**
     * 查询向量；已预先计算好的嵌入，提供时优先于 queryText
     */
    private float[] queryVector;

    /**
     * 返回的最相似结果数量上限（TopK）
     */
    private int topK;

    /**
     * 元数据过滤条件（键值对），用于按 agentId、来源等字段缩小检索范围
     */
    private Map<String, Object> filter;

    /**
     * Spring AI Filter 表达式字符串，如 {@code "agentId == 1 && userId == 2"}。
     * 非空时优先于 filter Map 和 indexName 推导的过滤条件。
     */
    private String filterExpression;
}
