package com.aizuda.snail.ai.vector.storage.vector.api;

import java.util.List;

/**
 * 向量存储：只接收显式 {@code indexName}，不负责推导命名（由 {@link IndexNameBuilder} 等完成）。
 */
public interface SnailAiVectorStore {

    String getType();

    void add(VectorAddRequest request);

    void delete(String indexName, List<String> ids);

    /**
     * 删除整个索引 / 集合 / 逻辑分区（语义依实现而定）。
     */
    void deleteByIndexName(String indexName);

    List<VectorSearchResult> search(VectorSearchRequest request);

    boolean test();
}
