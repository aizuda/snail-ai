package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * SearchRequest
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequest {

    /**
     * 显式索引名；与向量检索风格一致，优先级高于 filter 中的推导字段。
     */
    private String indexName;

    private String queryText;

    private int topK;

    private String filterExpression;

}
