package com.aizuda.snail.ai.search.storage.search.postgresql;

import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/** PostgreSQL 全文检索连接池工厂。 */
@Slf4j
public final class PgFullTextDataSourceFactory {

    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE_CONNECTIONS = 1;
    private static final long CONNECTION_TIMEOUT_MS = 30000L;
    private static final ConcurrentHashMap<String, JdbcTemplate> CACHE = new ConcurrentHashMap<>();

    private PgFullTextDataSourceFactory() {
    }

    public static JdbcTemplate getOrCreate(PgFullTextSettings settings) {
        settings.validate();
        String cacheKey = buildCacheKey(settings);
        return CACHE.computeIfAbsent(cacheKey, key -> new JdbcTemplate(createDataSource(settings, key)));
    }

    public static void closeAll() {
        CACHE.forEach((key, template) -> {
            if (template.getDataSource() instanceof HikariDataSource dataSource && !dataSource.isClosed()) {
                dataSource.close();
            }
        });
        CACHE.clear();
    }

    private static HikariDataSource createDataSource(PgFullTextSettings settings, String cacheKey) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(buildJdbcUrl(settings));
            config.setUsername(settings.getUsername());
            config.setPassword(settings.getPassword());
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(MAX_POOL_SIZE);
            config.setMinimumIdle(MIN_IDLE_CONNECTIONS);
            config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("PgFullText-" + cacheKey.substring(0, 12));
            log.info("创建 PostgreSQL 全文检索连接池: {}:{}/{} schema={}",
                    settings.getHost(), settings.getPort(), settings.getDatabase(), settings.getSchema());
            return new HikariDataSource(config);
        } catch (Exception e) {
            throw new SearchEngineException("创建 PostgreSQL 全文检索连接池失败", e);
        }
    }

    private static String buildJdbcUrl(PgFullTextSettings settings) {
        return "jdbc:postgresql://" + settings.getHost() + ":" + settings.getPort()
                + "/" + settings.getDatabase() + "?sslmode=" + settings.resolvedSslMode();
    }

    private static String buildCacheKey(PgFullTextSettings settings) {
        String raw = String.join("\u0000",
                settings.getHost(),
                String.valueOf(settings.getPort()),
                settings.getDatabase(),
                settings.getUsername(),
                String.valueOf(settings.getPassword()),
                settings.getSchema(),
                String.valueOf(settings.isSslEnabled()),
                settings.resolvedSslMode());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
