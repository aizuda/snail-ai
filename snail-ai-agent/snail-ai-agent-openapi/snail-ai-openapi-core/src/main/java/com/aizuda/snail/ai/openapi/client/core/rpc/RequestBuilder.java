package com.aizuda.snail.ai.openapi.client.core.rpc;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;

import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 客户端代理工厂
 *
 * @author opensnail
 * @date 2026-04-24
 */
public final class RequestBuilder {

    private RequestBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> iface, HttpClient httpClient,
                                    SnailAiOpenApiProperties openApiProperties,
                                    SnailAiAgentProperties  aiAgentProperties) {
        OpenApiHttpInvokeHandler handler = new OpenApiHttpInvokeHandler(httpClient, openApiProperties, aiAgentProperties);
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                handler);
    }

    public static HttpClient buildHttpClient(SnailAiOpenApiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }
}
