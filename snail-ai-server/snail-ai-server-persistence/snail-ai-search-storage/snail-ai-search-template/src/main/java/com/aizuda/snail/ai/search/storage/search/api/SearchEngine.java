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
     */
    void insert(SearchAddRequest request);

    /**
     * 按查询条件执行全文检索并返回匹配结果
     */
    List<SearchResult> search(SearchRequest request);

    /**
     * 按文档 ID 从指定索引批量删除
     */
    void delete(SearchDeleteRequest request);
}
