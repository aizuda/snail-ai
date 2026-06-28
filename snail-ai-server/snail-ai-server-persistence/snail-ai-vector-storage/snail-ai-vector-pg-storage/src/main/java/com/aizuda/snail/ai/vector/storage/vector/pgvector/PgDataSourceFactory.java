package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@link PgVectorSettings}（来自 DB 存储实例）创建 PostgreSQL 连接池。
 * <p>
 * 使用缓存机制复用 JdbcTemplate（内含 HikariCP 连接池），避免重复创建导致连接数暴增。
 */
@Slf4j
public final class PgDataSourceFactory {

    /**
     * JdbcTemplate 缓存：key = host:port/database/username
     */
    private static final ConcurrentHashMap<String, JdbcTemplate> CACHE = new ConcurrentHashMap<>();

    private PgDataSourceFactory() {
    }

    /**
     * 获取或创建 JdbcTemplate（复用连接池）
     */
    public static JdbcTemplate getOrCreateJdbcTemplate(PgVectorSettings config) {
        if (!config.isEnabled()) {
            throw new IllegalArgumentException("PostgreSQL vector settings are disabled");
        }
        String cacheKey = buildCacheKey(config);
        return CACHE.computeIfAbsent(cacheKey, k -> new JdbcTemplate(createNewDataSource(config, cacheKey)));
    }

    /**
     * 生成缓存 key
     */
    private static String buildCacheKey(PgVectorSettings config) {
        return String.format("%s:%d/%s/%s",
                config.getHost(), config.getPort(), config.getDatabase(), config.getUsername());
    }

    /**
     * 创建新的连接池
     */
    private static HikariDataSource createNewDataSource(PgVectorSettings config, String cacheKey) {
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
            hikariConfig.setPoolName("PgVector-" + cacheKey.hashCode());
            hikariConfig.setAutoCommit(true);
            hikariConfig.setConnectionTestQuery("SELECT 1");
            log.info("创建新的 PgVector 连接池: {} -> {}", cacheKey, hikariConfig.getPoolName());
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            log.error("Failed to create PostgreSQL DataSource for pgvector: {}", cacheKey, e);
            throw new SnailAiException("Failed to create PostgreSQL DataSource for pgvector, {}", e.getMessage());
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

    /**
     * 关闭所有连接池（应用关闭时调用）
     */
    public static void closeAll() {
        CACHE.forEach((key, jt) -> {
            if (jt.getDataSource() instanceof HikariDataSource ds && !ds.isClosed()) {
                ds.close();
                log.info("关闭 PgVector 连接池: {}", key);
            }
        });
        CACHE.clear();
    }

}
