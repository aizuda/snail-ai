package com.aizuda.snail.ai.vector.storage.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量库类型枚举，标识当前启用的向量存储实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum VectorStoreType {

    /**
     * PGVector（PostgreSQL pgvector 扩展）
     */
    PG_VECTOR("PG_VECTOR"),
    /**
     * Milvus 向量数据库
     */
    MILVUS("MILVUS"),
    /**
     * Elasticsearch 向量检索
     */
    ELASTICSEARCH("ELASTICSEARCH"),
    /**
     * MongoDB 向量检索
     */
    MONGO("MONGO");

    /**
     * 类型标识，用于配置匹配与实例选择
     */
    private final String type;
}
