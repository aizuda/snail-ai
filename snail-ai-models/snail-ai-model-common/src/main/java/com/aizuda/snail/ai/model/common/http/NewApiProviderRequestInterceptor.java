package com.aizuda.snail.ai.model.common.http;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 为 CodeLink 中转请求追加 Snail AI 来源标识，并兼容历史 New API 配置。
 *
 * @author opensnail
 * @since 2026-11-15
 */
public final class NewApiProviderRequestInterceptor implements Interceptor {

    public static final String PROVIDER_KEY = "codelink";
    public static final String LEGACY_CODELINK_PROVIDER_KEY = "aicodelink";
    public static final String LEGACY_NEW_API_PROVIDER_KEY = "new-api";
    public static final String SOURCE_QUERY_PARAMETER = "source";
    public static final String SOURCE_QUERY_VALUE = "snail-ai";

    private static final Set<String> SUPPORTED_PROVIDER_KEYS = Set.of(
            PROVIDER_KEY, LEGACY_CODELINK_PROVIDER_KEY, LEGACY_NEW_API_PROVIDER_KEY);

    private static final NewApiProviderRequestInterceptor INSTANCE = new NewApiProviderRequestInterceptor();

    private NewApiProviderRequestInterceptor() {
    }

    public static NewApiProviderRequestInterceptor instance() {
        return INSTANCE;
    }

    public static boolean supports(String providerKey) {
        if (providerKey == null) {
            return false;
        }
        return SUPPORTED_PROVIDER_KEYS.contains(providerKey.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 仅为 CodeLink 和历史 New API 供应商请求追加来源标识，其他供应商保持原 URL 不变。
     *
     * @param providerKey 供应商标识
     * @param requestUrl  请求 URL
     * @return 处理后的请求 URL
     */
    public static String mark(String providerKey, String requestUrl) {
        Objects.requireNonNull(requestUrl, "requestUrl");
        if (!supports(providerKey)) {
            return requestUrl;
        }
        return appendSourceMarker(HttpUrl.get(requestUrl)).toString();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl markedUrl = appendSourceMarker(request.url());
        if (markedUrl.equals(request.url())) {
            return chain.proceed(request);
        }
        return chain.proceed(request.newBuilder().url(markedUrl).build());
    }

    private static HttpUrl appendSourceMarker(HttpUrl requestUrl) {
        return requestUrl.newBuilder()
                .setQueryParameter(SOURCE_QUERY_PARAMETER, SOURCE_QUERY_VALUE)
                .build();
    }
}
