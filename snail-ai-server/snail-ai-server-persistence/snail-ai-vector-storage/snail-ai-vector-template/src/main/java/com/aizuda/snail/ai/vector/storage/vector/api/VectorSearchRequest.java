package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorSearchRequest {

    /**
     * 索引或集合名称；非空时优先于 filter 中的 ragId 推导。
     */
    private String indexName;

    private String queryText;

    private float[] queryVector;

    private int topK;

    private Map<String, Object> filter;

    /**
     * Spring AI Filter 表达式字符串，如 {@code "agentId == 1 && userId == 2"}。
     * 非空时优先于 filter Map 和 indexName 推导的过滤条件。
     */
    private String filterExpression;
}
