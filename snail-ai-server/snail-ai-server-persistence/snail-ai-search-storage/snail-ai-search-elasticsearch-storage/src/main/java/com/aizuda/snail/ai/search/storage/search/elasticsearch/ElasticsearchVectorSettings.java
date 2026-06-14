package com.aizuda.snail.ai.search.storage.search.elasticsearch;

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

    /** 实际索引*/
    private String indexPrefix = "snail_ai_search";

}
