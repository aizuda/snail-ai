package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * 按 {@link PgVectorSettings}（来自 DB 存储实例）创建 PostgreSQL 连接池。
 */
@Slf4j
public final class PgDataSourceFactory {

    private PgDataSourceFactory() {
    }

    /**
     * 为单次向量库访问创建独立 Hikari 连接池（每个存储实例各自连接参数）。
     */
    public static DataSource createDataSource(PgVectorSettings config) {
        if (!config.isEnabled()) {
            throw new IllegalArgumentException("PostgreSQL vector settings are disabled");
        }
        try {
            HikariConfig hikariConfig = new HikariConfig();
            String jdbcUrl = buildJdbcUrl(config);
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(Math.max(2, config.getMaxPoolSize()));
            hikariConfig.setMinimumIdle(Math.max(1, config.getMinIdleConnections()));
            hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
            hikariConfig.setIdleTimeout(config.getIdleTimeoutMs());
            hikariConfig.setMaxLifetime(config.getMaxLifetimeMs());
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            hikariConfig.setPoolName("PgVector-Instance-" + System.identityHashCode(config));
            hikariConfig.setAutoCommit(true);
            hikariConfig.setConnectionTestQuery("SELECT 1");
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            log.error("Failed to create PostgreSQL DataSource for pgvector", e);
            throw new SnailAiException("Failed to create PostgreSQL DataSource for pgvector", e);
        }
    }

    private static String buildJdbcUrl(PgVectorSettings config) {
        StringBuilder url = new StringBuilder("jdbc:postgresql://");
        url.append(config.getHost()).append(":").append(config.getPort());
        url.append("/").append(config.getDatabase());
        if (config.isSslEnabled()) {
            url.append("?sslmode=").append(config.getSslMode());
        } else {
            url.append("?sslmode=disable");
        }
        return url.toString();
    }
}
