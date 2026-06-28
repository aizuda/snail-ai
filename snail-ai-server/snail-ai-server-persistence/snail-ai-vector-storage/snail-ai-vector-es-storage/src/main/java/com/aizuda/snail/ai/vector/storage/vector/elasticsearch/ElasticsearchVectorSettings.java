package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import lombok.Data;

/**
 * Elasticsearch 向量索引连接与映射参数，来自存储实例 {@code config} JSON。
 */
@Data
public class ElasticsearchVectorSettings {

    private boolean enabled = true;

    private String host = "localhost";

    private int port = 9200;

    private String scheme = "http";

    private String username;

    private String password;

    /** 是否关闭 HTTPS 证书和主机名校验，仅用于内网自签名证书 */
    private boolean sslVerificationDisabled = false;

    /** 实际索引名：{indexPrefix}_{ragId} */
    private String indexPrefix = "snail_ai_vector";

    /** cosine | dot_product | l2_norm */
    private String similarity = "cosine";

    private int numCandidates = 100;
}
