package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import lombok.Data;

/**
 * PostgreSQL 向量库连接与表/索引参数，由存储实例 {@code config} JSON 解析得到（不依赖 application.yml）。
 */
@Data
public class PgVectorSettings {

    private boolean enabled = true;

    private String host = "localhost";

    private int port = 5432;

    private String database = "snail_ai";

    private String username = "postgres";

    private String password = "";

    private boolean sslEnabled = false;

    private String sslMode = "disable";

    private int maxPoolSize = 20;

    private int minIdleConnections = 5;

    private long connectionTimeoutMs = 30000;

    private long idleTimeoutMs = 600000;

    private long maxLifetimeMs = 1800000;

    /** Spring AI 默认表名；存量若使用旧表需迁移至该表结构 */
    private String vectorTableName = "vector_store";

    private int defaultDimension = 1024;

    private boolean hnswIndexEnabled = true;

    private int hnswEfConstruction = 64;

    private int hnswEfSearch = 32;
}
