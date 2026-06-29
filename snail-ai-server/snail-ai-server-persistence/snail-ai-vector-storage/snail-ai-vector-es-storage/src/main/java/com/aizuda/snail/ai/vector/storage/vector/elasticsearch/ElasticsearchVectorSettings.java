package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import lombok.Data;

/**
 * Elasticsearch 向量索引连接与映射参数，来自存储实例 {@code config} JSON。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class ElasticsearchVectorSettings {

    /** 是否启用该 Elasticsearch 向量库配置 */
    private boolean enabled = true;

    /** 主机地址，支持直接填写带 scheme 的完整 endpoint */
    private String host = "localhost";

    /** 端口 */
    private int port = 9200;

    /** 连接协议：http 或 https */
    private String scheme = "http";

    /** 认证用户名（为空表示不启用 Basic 认证） */
    private String username;

    /** 认证密码 */
    private String password;

    /** 是否关闭 HTTPS 证书和主机名校验，仅用于内网自签名证书 */
    private boolean sslVerificationDisabled = false;

    /** 实际索引名：{indexPrefix}_{ragId} */
    private String indexPrefix = "snail_ai_vector";

    /** cosine | dot_product | l2_norm */
    private String similarity = "cosine";

    /** kNN 检索的候选数量，越大召回越准但越慢 */
    private int numCandidates = 100;
}
