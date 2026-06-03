package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@link ElasticsearchVectorSettings}（来自 DB 存储实例）创建 Elasticsearch HTTP 客户端。
 * <p>
 * 使用缓存机制复用客户端，避免重复创建导致连接数暴增。
 *
 * @author opensnail
 * @date 2026-06-03
 */
@Slf4j
public final class EsClientFactory {

    /**
     * 客户端缓存：key = scheme://host:port/username
     */
    private static final ConcurrentHashMap<String, Rest5Client> CLIENT_CACHE = new ConcurrentHashMap<>();

    private EsClientFactory() {
    }

    /**
     * 获取或创建 Rest5Client（复用客户端连接）
     */
    public static Rest5Client getOrCreateClient(ElasticsearchVectorSettings config) {
        if (!config.isEnabled()) {
            throw new IllegalArgumentException("Elasticsearch vector settings are disabled");
        }
        String cacheKey = buildCacheKey(config);
        return CLIENT_CACHE.computeIfAbsent(cacheKey, k -> createNewClient(config, cacheKey));
    }

    /**
     * 生成缓存 key
     */
    private static String buildCacheKey(ElasticsearchVectorSettings config) {
        String username = config.getUsername() != null ? config.getUsername() : "";
        return String.format("%s://%s:%d/%s",
                config.getScheme(), config.getHost(), config.getPort(), username);
    }

    /**
     * 创建新的客户端
     */
    private static Rest5Client createNewClient(ElasticsearchVectorSettings config, String cacheKey) {
        try {
            String uri = config.getScheme() + "://" + config.getHost() + ":" + config.getPort();
            var builder = Rest5Client.builder(URI.create(uri));
            if (config.getUsername() != null && !config.getUsername().isBlank()
                    && config.getPassword() != null) {
                String raw = config.getUsername() + ":" + config.getPassword();
                String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                Header[] headers = new Header[]{new BasicHeader("Authorization", "Basic " + b64)};
                builder.setDefaultHeaders(headers);
            }
            log.info("创建新的 Elasticsearch 客户端: {}", cacheKey);
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create Elasticsearch Rest5Client: {}", cacheKey, e);
            throw new VectorStoreException("构建 Elasticsearch Rest5Client 失败", e);
        }
    }

    /**
     * 关闭指定配置的客户端（存储实例删除或配置变更时调用）
     */
    public static void closeClient(ElasticsearchVectorSettings config) {
        String cacheKey = buildCacheKey(config);
        Rest5Client client = CLIENT_CACHE.remove(cacheKey);
        if (client != null) {
            try {
                client.close();
                log.info("关闭 Elasticsearch 客户端: {}", cacheKey);
            } catch (Exception e) {
                log.warn("关闭 Elasticsearch 客户端失败: {}", cacheKey, e);
            }
        }
    }

    /**
     * 关闭所有客户端（应用关闭时调用）
     */
    public static void closeAll() {
        CLIENT_CACHE.forEach((key, client) -> {
            try {
                client.close();
                log.info("关闭 Elasticsearch 客户端: {}", key);
            } catch (Exception e) {
                log.warn("关闭 Elasticsearch 客户端失败: {}", key, e);
            }
        });
        CLIENT_CACHE.clear();
    }

    /**
     * 获取当前缓存的客户端数量（用于监控/调试）
     */
    public static int getCachedClientCount() {
        return CLIENT_CACHE.size();
    }
}
