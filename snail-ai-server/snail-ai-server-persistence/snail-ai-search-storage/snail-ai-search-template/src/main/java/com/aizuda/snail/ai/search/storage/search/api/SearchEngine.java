package com.aizuda.snail.ai.search.storage.search.api;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.List;

/**
 * SearchEngine
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface SearchEngine {

    void insert(SearchAddRequest request);

    List<SearchResult> search(SearchRequest request);

    void delete(SearchDeleteRequest request);
}
