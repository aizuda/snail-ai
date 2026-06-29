package com.aizuda.snail.ai.search.storage.search.elasticsearch;

import lombok.Data;

/**
 * Elasticsearch 向量索引连接与映射参数，来自存储实例 {@code config} JSON。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class ElasticsearchVectorSettings {

    /** 是否启用该 Elasticsearch 全文检索配置 */
    private boolean enabled = true;

    /** 主机地址 */
    private String host = "localhost";

    /** 端口 */
    private int port = 9200;

    /** 连接协议：http 或 https */
    private String scheme = "http";

    /** 认证用户名（为空表示不启用 Basic 认证） */
    private String username;

    /** 认证密码 */
    private String password;

    /** 索引名前缀 */
    private String indexPrefix = "snail_ai_search";

}
