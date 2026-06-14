package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;

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

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final String URL_SCHEME_SEPARATOR = "://";

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
        return String.format("%s/%s/sslVerificationDisabled=%s",
                buildEndpointUri(config), username, config.isSslVerificationDisabled());
    }

    /**
     * 创建新的客户端
     */
    private static Rest5Client createNewClient(ElasticsearchVectorSettings config, String cacheKey) {
        try {
            URI endpointUri = buildEndpointUri(config);
            var builder = Rest5Client.builder(endpointUri);
            applySslConfig(builder, config, endpointUri);
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

    private static URI buildEndpointUri(ElasticsearchVectorSettings config) {
        String host = requireHost(config);
        if (hasHttpScheme(host)) {
            return buildUriFromEndpoint(host, config.getPort());
        }
        return buildUri(normalizeScheme(config.getScheme()), host, config.getPort(), null);
    }

    private static String requireHost(ElasticsearchVectorSettings config) {
        if (config.getHost() == null || config.getHost().isBlank()) {
            throw new IllegalArgumentException("Elasticsearch host must not be blank");
        }
        return config.getHost().trim();
    }

    private static boolean hasHttpScheme(String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        return lowerHost.startsWith(HTTP_SCHEME + URL_SCHEME_SEPARATOR)
                || lowerHost.startsWith(HTTPS_SCHEME + URL_SCHEME_SEPARATOR);
    }

    private static URI buildUriFromEndpoint(String endpoint, int fallbackPort) {
        URI uri = URI.create(endpoint);
        String scheme = normalizeScheme(uri.getScheme());
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Elasticsearch endpoint host is invalid: " + endpoint);
        }
        int port = uri.getPort() > 0 ? uri.getPort() : fallbackPort;
        return buildUri(scheme, host, port, uri.getPath());
    }

    private static String normalizeScheme(String scheme) {
        String normalizedScheme = scheme == null || scheme.isBlank()
                ? HTTP_SCHEME
                : scheme.trim().toLowerCase(Locale.ROOT);
        if (!HTTP_SCHEME.equals(normalizedScheme) && !HTTPS_SCHEME.equals(normalizedScheme)) {
            throw new IllegalArgumentException("Elasticsearch scheme only supports http or https: " + scheme);
        }
        return normalizedScheme;
    }

    private static URI buildUri(String scheme, String host, int port, String path) {
        try {
            return new URI(scheme, null, host, port, normalizePath(path), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Elasticsearch endpoint: " + scheme + URL_SCHEME_SEPARATOR
                    + host + ":" + port, e);
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return null;
        }
        return path;
    }

    private static void applySslConfig(co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder builder,
                                       ElasticsearchVectorSettings config,
                                       URI endpointUri) throws Exception {
        if (!HTTPS_SCHEME.equalsIgnoreCase(endpointUri.getScheme()) || !config.isSslVerificationDisabled()) {
            return;
        }
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        builder.setConnectionManagerCallback(connectionManagerBuilder -> connectionManagerBuilder
                .setTlsStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE)));
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
}
