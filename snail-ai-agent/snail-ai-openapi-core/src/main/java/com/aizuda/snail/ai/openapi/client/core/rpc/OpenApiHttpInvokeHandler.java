package com.aizuda.snail.ai.openapi.client.core.rpc;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;
import com.aizuda.snail.ai.openapi.client.core.annotation.PathParam;
import com.aizuda.snail.ai.openapi.client.core.annotation.QueryParam;
import com.aizuda.snail.ai.openapi.client.core.listener.SseEventListener;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;
import tools.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

/**
 * OpenAPI HTTP 动态代理处理器
 * <p>
 * 基于 JDK 动态代理，解析 @OpenApiMapping 注解，
 * 自动构建 HTTP 请求并发送，支持 SSE 流式响应。
 *
 * @author opensnail
 * @date 2026-04-24
 */
public class OpenApiHttpInvokeHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenApiHttpInvokeHandler.class);
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    private final HttpClient httpClient;
    private final SnailAiOpenApiProperties openApiProperties;
    private final SnailAiAgentProperties agentProperties;
    private final String baseUrl;

    public OpenApiHttpInvokeHandler(HttpClient httpClient, SnailAiOpenApiProperties openApiProperties, SnailAiAgentProperties agentProperties) {
        this.httpClient = httpClient;
        this.openApiProperties = openApiProperties;
        this.agentProperties = agentProperties;
        this.baseUrl = buildBaseUrl();

        log.info("OpenAPI HTTP client initialized - Base URL: {}, ConnectTimeout: {}ms, ReadTimeout: {}ms, ChatTimeout: {}ms",
                baseUrl, openApiProperties.getConnectTimeoutMs(),
                openApiProperties.getReadTimeoutMs(), openApiProperties.getChatTimeoutMs());
    }

    /**
     * 构建基础 URL
     * 优先使用 openApiProperties 的配置，若未配置则使用 agentProperties
     */
    private String buildBaseUrl() {
        String host = openApiProperties.getServerHost();
        if (host == null || host.trim().isEmpty()) {
            host = agentProperties.getServerHost();
        }

        String protocol = openApiProperties.isHttps() ? "https" : "http";
        int port = openApiProperties.getWebPort();
        String prefix = openApiProperties.getPrefix();

        // 移除 host 末尾的斜杠
        host = host.replaceAll("/$", "");

        // 构建完整 URL: protocol://host:port/prefix
        StringBuilder url = new StringBuilder(protocol).append("://").append(host);

        // 只在非默认端口时添加端口号
        if ((openApiProperties.isHttps() && port != 443) || (!openApiProperties.isHttps() && port != 80)) {
            url.append(":").append(port);
        }

        // 添加前缀（确保以 / 开头）
        if (prefix != null && !prefix.trim().isEmpty()) {
            if (!prefix.startsWith("/")) {
                url.append("/");
            }
            url.append(prefix);
        }

        return url.toString();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        OpenApiMapping mapping = method.getAnnotation(OpenApiMapping.class);
        if (mapping == null) {
            throw new IllegalStateException("Method " + method.getName() + " missing @OpenApiMapping");
        }

        String path = mapping.path();
        OpenApiMapping.HttpMethod httpMethod = mapping.method();
        Parameter[] parameters = method.getParameters();

        Object requestBody = null;
        SseEventListener sseListener = null;
        StringJoiner queryParams = new StringJoiner("&");

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Parameter param = parameters[i];
                Object arg = args[i];

                PathParam pathParam = param.getAnnotation(PathParam.class);
                if (pathParam != null) {
                    path = path.replace("{" + pathParam.value() + "}", String.valueOf(arg));
                    continue;
                }

                QueryParam queryParam = param.getAnnotation(QueryParam.class);
                if (queryParam != null) {
                    appendQueryParams(queryParams, queryParam.value(), arg);
                    continue;
                }

                if (arg instanceof SseEventListener) {
                    sseListener = (SseEventListener) arg;
                    continue;
                }

                if ((httpMethod == OpenApiMapping.HttpMethod.GET || httpMethod == OpenApiMapping.HttpMethod.DELETE) && arg != null) {
                    appendBeanQueryParams(queryParams, arg);
                    continue;
                }

                requestBody = arg;
            }
        }

        String url = buildUrl(path, queryParams);

        if (sseListener != null) {
            executeSseRequest(url, httpMethod, requestBody, sseListener);
            return null;
        }

        return executeRequest(url, httpMethod, requestBody, method.getGenericReturnType());
    }

    /**
     * 构建完整请求 URL（包含路径和查询参数）
     */
    private String buildUrl(String path, StringJoiner queryParams) {
        // 确保路径以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String url = baseUrl + path;
        String query = queryParams.toString();
        if (!query.isEmpty()) {
            url += "?" + query;
        }
        return url;
    }

    private void appendQueryParams(StringJoiner queryParams, String key, Object value) {
        if (value == null) {
            return;
        }
        queryParams.add(encode(key) + "=" + encode(String.valueOf(value)));
    }

    private void appendBeanQueryParams(StringJoiner queryParams, Object bean) {
        Map<String, Object> paramMap = MAPPER.convertValue(bean, new TypeReference<>() {
        });
        paramMap.forEach((key, value) -> appendQueryParams(queryParams, key, value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 构建基础 HTTP 请求（包含公共请求头和超时配置）
     */
    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(openApiProperties.getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Snail-Ai-App-Id", agentProperties.getAppId())
                .header("Snail-Ai-Token", agentProperties.getToken());
    }

    /**
     * 执行普通 HTTP 请求（非流式）
     */
    private Object executeRequest(String url, OpenApiMapping.HttpMethod method,
                                  Object body, Type returnType) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Executing {} request: {}", method, url);
        }

        HttpRequest.Builder builder = baseRequest(url);

        switch (method) {
            case GET -> builder.GET();
            case POST -> builder.POST(bodyPublisher(body));
            case PUT -> builder.PUT(bodyPublisher(body));
            case DELETE -> builder.DELETE();
        }


        HttpResponse<String> response = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            log.error("OpenAPI request failed: HTTP {} - URL: {} - Response: {}",
                    response.statusCode(), url, response.body());
            throw new SnailAiException("OpenAPI request failed: HTTP " + response.statusCode()
                    + " - " + response.body());
        }

        if (log.isDebugEnabled()) {
            log.debug("Request succeeded: HTTP {} - URL: {}", response.statusCode(), url);
        }

        JavaType javaType = TypeFactory.createDefaultInstance().constructType(returnType);
        return MAPPER.readValue(response.body(), javaType);
    }

    /**
     * 执行 SSE 流式请求
     */
    private void executeSseRequest(String url, OpenApiMapping.HttpMethod method,
                                   Object body, SseEventListener listener) {
        if (log.isDebugEnabled()) {
            log.debug("Executing SSE {} request: {}", method, url);
        }
        
        HttpRequest.Builder builder = baseRequest(url)
                .timeout(Duration.ofMillis(openApiProperties.getChatTimeoutMs()))
                .header("Accept", "text/event-stream");

        switch (method) {
            case POST -> builder.POST(bodyPublisher(body));
            default -> builder.GET();
        }

        httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        log.error("SSE request failed: HTTP {} - URL: {}", response.statusCode(), url);
                        listener.onError("HTTP " + response.statusCode());
                        return;
                    }
                    
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                        SseResponseHandler.parse(reader, listener);
                    } catch (Exception e) {
                        log.error("SSE stream processing error: URL: {} - Error: {}", url, e.getMessage(), e);
                        listener.onError(e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("SSE request exception: URL: {} - Error: {}", url, ex.getMessage(), ex);
                    listener.onError(ex.getMessage());
                    return null;
                });
    }

    /**
     * 将请求体对象序列化为 HTTP BodyPublisher
     */
    private HttpRequest.BodyPublisher bodyPublisher(Object body) {
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        try {
            String json = MAPPER.writeValueAsString(body);
            return HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to serialize request body: {}", body.getClass().getName(), e);
            throw new SnailAiException("Failed to serialize request body", e);
        }
    }
}
