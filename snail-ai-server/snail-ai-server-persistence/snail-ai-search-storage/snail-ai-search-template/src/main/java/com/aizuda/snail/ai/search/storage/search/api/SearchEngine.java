package com.aizuda.snail.ai.search.storage.search.api;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.List;

/**
 * 全文检索引擎抽象，统一文档写入、检索与删除入口，由具体引擎（如 Elasticsearch）实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface SearchEngine {

    /**
     * 批量写入文档到指定索引（索引不存在时自动创建）
     *
     * @param request 写入请求（含索引名与文档列表）
     */
    void insert(SearchAddRequest request);

    /**
     * 按查询条件执行全文检索并返回匹配结果
     *
     * @param request 检索请求（含索引名、查询文本、topK 与过滤条件）
     * @return 命中结果列表，无匹配或索引不存在时返回空列表
     */
    List<SearchResult> search(SearchRequest request);

    /**
     * 按文档 ID 从指定索引批量删除
     *
     * @param request 删除请求（含索引名与文档 ID 列表）
     */
    void delete(SearchDeleteRequest request);
}
