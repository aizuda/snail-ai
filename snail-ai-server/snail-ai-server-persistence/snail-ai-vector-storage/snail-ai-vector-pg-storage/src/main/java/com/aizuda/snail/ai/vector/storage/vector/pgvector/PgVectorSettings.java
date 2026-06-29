package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import lombok.Data;

/**
 * PostgreSQL 向量库连接与表/索引参数，由存储实例 {@code config} JSON 解析得到（不依赖 application.yml）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class PgVectorSettings {

    /** 是否启用该 PostgreSQL 向量库配置 */
    private boolean enabled = true;

    /** 数据库主机地址 */
    private String host = "localhost";

    /** 数据库端口 */
    private int port = 5432;

    /** 数据库名 */
    private String database = "snail_ai";

    /** 登录用户名 */
    private String username = "postgres";

    /** 登录密码 */
    private String password = "";

    /** 是否启用 SSL 连接 */
    private boolean sslEnabled = false;

    /** SSL 模式（disable / require / verify-full 等），sslEnabled 为 false 时按 disable 处理 */
    private String sslMode = "disable";

    /** HikariCP 连接池最大连接数 */
    private int maxPoolSize = 20;

    /** HikariCP 连接池最小空闲连接数 */
    private int minIdleConnections = 5;

    /** 获取连接超时时间（毫秒） */
    private long connectionTimeoutMs = 30000;

    /** 连接空闲超时时间（毫秒） */
    private long idleTimeoutMs = 600000;

    /** 连接最大存活时间（毫秒） */
    private long maxLifetimeMs = 1800000;

    /** Spring AI 默认表名；存量若使用旧表需迁移至该表结构 */
    private String vectorTableName = "vector_store";

    /** 未显式指定 embedding 维度时使用的默认向量维度 */
    private int defaultDimension = 1024;

    /** 是否启用 HNSW 索引 */
    private boolean hnswIndexEnabled = true;

    /** HNSW 构建参数 ef_construction，越大构建越慢、召回越准 */
    private int hnswEfConstruction = 64;

    /** HNSW 检索参数 ef_search，越大检索越慢、召回越准 */
    private int hnswEfSearch = 32;
}
