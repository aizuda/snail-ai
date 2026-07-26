package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 全文检索请求参数，描述一次检索的目标索引、查询文本、返回条数与过滤条件。
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

    /**
     * 查询文本
     */
    private String queryText;

    /**
     * 返回结果条数上限
     */
    private int topK;

    /**
     * 过滤表达式（如按元数据字段筛选）
     */
    private String filterExpression;

}
