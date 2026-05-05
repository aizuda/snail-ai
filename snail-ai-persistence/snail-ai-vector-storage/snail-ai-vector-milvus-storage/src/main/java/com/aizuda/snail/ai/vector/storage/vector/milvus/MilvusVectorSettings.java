package com.aizuda.snail.ai.vector.storage.vector.milvus;

import lombok.Data;

/**
 * Milvus 连接与集合/索引参数，来自存储实例 {@code config} JSON。
 */
@Data
public class MilvusVectorSettings {

    private boolean enabled = true;

    private String host = "localhost";

    private int port = 19530;

    private String token;

    private String database = "default";

    /** 实际 collection：{collectionPrefix}_{ragId} */
    private String collectionPrefix = "snail_rag_vector";

    /** COSINE | L2 | IP */
    private String metricType = "COSINE";

    /** IVF_FLAT | IVF_SQ8 | HNSW | AUTOINDEX */
    private String indexType = "AUTOINDEX";

    private int nlist = 1024;

    private int nprobe = 16;
}
