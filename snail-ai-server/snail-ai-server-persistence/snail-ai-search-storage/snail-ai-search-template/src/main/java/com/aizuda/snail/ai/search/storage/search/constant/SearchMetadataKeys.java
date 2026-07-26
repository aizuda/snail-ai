package com.aizuda.snail.ai.search.storage.search.constant;

/**
 * 全文检索文档元数据字段名常量，统一写入文档时的元数据键名，避免散落的魔法字符串。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public final class SearchMetadataKeys {

    private SearchMetadataKeys() {
    }

    // RAG / 文档检索相关
    public static final String INDEX_NAME = "indexName";
    public static final String RAG_ID = "ragId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String CHUNK_ID = "chunkId";
    public static final String SOURCE_TYPE = "sourceType";

    // 记忆相关
    public static final String MEMORY_ID = "memoryId";
    public static final String AGENT_ID = "agentId";
    public static final String USER_ID = "userId";
}
