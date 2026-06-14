package com.aizuda.snail.ai.vector.storage.vector.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 {@link MilvusVectorSettings}（来自 DB 存储实例）创建 Milvus gRPC 客户端。
 * <p>
 * 使用缓存机制复用客户端，避免重复创建导致连接数暴增。
 *
 * @author opensnail
 * @date 2026-06-03
 */
@Slf4j
public final class MilvusClientFactory {

    /**
     * 客户端缓存：key = host:port/database/token
     */
    private static final ConcurrentHashMap<String, MilvusServiceClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    private MilvusClientFactory() {
    }

    /**
     * 获取或创建 MilvusServiceClient（复用客户端连接）
     */
    public static MilvusServiceClient getOrCreateClient(MilvusVectorSettings config) {
        if (!config.isEnabled()) {
            throw new IllegalArgumentException("Milvus vector settings are disabled");
        }
        String cacheKey = buildCacheKey(config);
        return CLIENT_CACHE.computeIfAbsent(cacheKey, k -> createNewClient(config, cacheKey));
    }

    /**
     * 生成缓存 key
     */
    private static String buildCacheKey(MilvusVectorSettings config) {
        String token = config.getToken() != null ? String.valueOf(config.getToken().hashCode()) : "";
        return String.format("%s:%d/%s/%s",
                config.getHost(), config.getPort(), config.getDatabase(), token);
    }

    /**
     * 创建新的客户端
     */
    private static MilvusServiceClient createNewClient(MilvusVectorSettings config, String cacheKey) {
        ConnectParam.Builder cb = ConnectParam.newBuilder()
                .withHost(config.getHost())
                .withPort(config.getPort())
                .withDatabaseName(config.getDatabase());
        if (config.getToken() != null && !config.getToken().isBlank()) {
            cb.withAuthorization(config.getToken());
        }
        log.info("创建新的 Milvus 客户端: {}", cacheKey);
        return new MilvusServiceClient(cb.build());
    }

    /**
     * 关闭指定配置的客户端（存储实例删除或配置变更时调用）
     */
    public static void closeClient(MilvusVectorSettings config) {
        String cacheKey = buildCacheKey(config);
        MilvusServiceClient client = CLIENT_CACHE.remove(cacheKey);
        if (client != null) {
            try {
                client.close();
                log.info("关闭 Milvus 客户端: {}", cacheKey);
            } catch (Exception e) {
                log.warn("关闭 Milvus 客户端失败: {}", cacheKey, e);
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
                log.info("关闭 Milvus 客户端: {}", key);
            } catch (Exception e) {
                log.warn("关闭 Milvus 客户端失败: {}", key, e);
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
