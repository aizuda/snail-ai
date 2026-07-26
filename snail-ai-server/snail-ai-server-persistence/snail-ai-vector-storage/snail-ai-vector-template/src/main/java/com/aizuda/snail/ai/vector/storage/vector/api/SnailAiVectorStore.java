package com.aizuda.snail.ai.vector.storage.vector.api;

import java.util.List;

/**
 * 向量存储：只接收显式 {@code indexName}，不负责推导命名（由 {@link IndexNameBuilder} 等完成）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface SnailAiVectorStore {

    /**
     * 获取向量库类型标识（如 PG_VECTOR、MILVUS、ELASTICSEARCH）。
     *
     * @return 向量库类型标识
     */
    String getType();

    /**
     * 写入一批向量文档到指定索引 / 集合。
     *
     * @param request 向量写入请求（含索引名与文档列表）
     */
    void add(VectorAddRequest request);

    /**
     * 按文档 ID 列表从指定索引 / 集合删除向量。
     *
     * @param indexName 索引或集合名称
     * @param ids       待删除的文档 ID 列表
     */
    void delete(String indexName, List<String> ids);

    /**
     * 删除整个索引 / 集合 / 逻辑分区（语义依实现而定）。
     *
     * @param indexName 索引或集合名称
     */
    void deleteByIndexName(String indexName);

    /**
     * 执行向量相似度检索。
     *
     * @param request 向量检索请求（索引、查询文本/向量、TopK 与过滤条件）
     * @return 命中结果列表（按相似度排序）
     */
    List<VectorSearchResult> search(VectorSearchRequest request);

    /**
     * 连通性测试，校验向量库是否可正常访问。
     *
     * @return 可用返回 true
     */
    boolean test();
}
