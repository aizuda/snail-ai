package com.aizuda.snail.ai.agent.core.tool;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * 内置 HTTP 工具 — 支持 LLM 直接发起 HTTP 请求调用远程接口
 * <p>
 * 注入方式与 ShellTool 一致：ToolCallbacks.from(new HttpTool(...))
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
public class HttpTool {

    private static final int MAX_RESPONSE_LENGTH = 50000;
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String HEADER_HOST = "host";

    private final long timeoutMs;

    private final HttpClient httpClient;

    public HttpTool(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * 发起 HTTP 请求（支持 GET/POST），用于调用外部 API、抓取远程数据或发送 Webhook。
     *
     * @param url         请求 URL，必须以 http:// 或 https:// 开头
     * @param method      HTTP 方法：GET 或 POST（可选，默认 GET）
     * @param body        请求体（仅 POST，通常为 JSON 字符串，可选）
     * @param contentType Content-Type 请求头（可选，默认 application/json）
     * @param headers     附加请求头，多个以换行分隔，格式 Header:Value（可选）
     * @return 响应文本（含状态码与响应体，超长会截断）；失败时以 "Error:" 开头
     */
    @Tool(name = "http_request", description = "Make an HTTP request to a remote API. Supports GET and POST methods. "
            + "Can be used to call external APIs, fetch remote data, send webhook notifications, etc.")
    public String request(
            @ToolParam(description = "Request URL, must start with http:// or https://") String url,
            @ToolParam(description = "HTTP method: GET or POST", required = false) String method,
            @ToolParam(description = "Request body (POST only, typically a JSON string)", required = false) String body,
            @ToolParam(description = "Content-Type header (optional, defaults to application/json)", required = false) String contentType,
            @ToolParam(description = "Additional headers, format: Header1:Value1\\nHeader2:Value2", required = false) String headers) {
        log.info("http_request url:{}", url);

        if (StrUtil.isBlank(url)) {
            return "Error: URL cannot be empty";
        }

        String trimmedUrl = url.trim();
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return "Error: URL 必须以 http:// 或 https:// 开头";
        }

        String httpMethod = StrUtil.isNotBlank(method)
                ? method.trim().toUpperCase() : METHOD_GET;
        if (!METHOD_GET.equals(httpMethod) && !METHOD_POST.equals(httpMethod)) {
            return "Error: 仅支持 GET 和 POST 方法";
        }

        try {
            URI uri = URI.create(trimmedUrl);
            validatePublicHttpTarget(uri);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs));

            String effectiveContentType = StrUtil.isNotBlank(contentType)
                    ? contentType.trim() : DEFAULT_CONTENT_TYPE;

            if (StrUtil.isNotBlank(headers)) {
                for (String headerLine : headers.split("\\n")) {
                    String trimmed = headerLine.trim();
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx <= 0 || colonIdx >= trimmed.length() - 1) {
                        continue;
                    }
                    String key = trimmed.substring(0, colonIdx).trim();
                    String value = trimmed.substring(colonIdx + 1).trim();
                    if (HEADER_HOST.equalsIgnoreCase(key)) {
                        return "Error: 不允许覆盖 Host 请求头";
                    }
                    requestBuilder.header(key, value);
                }
            }

            if (METHOD_POST.equals(httpMethod)) {
                String requestBody = (body != null) ? body : "";
                requestBuilder.header("Content-Type", effectiveContentType);
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
            } else {
                requestBuilder.GET();
            }

            HttpRequest httpRequest = requestBuilder.build();
            log.debug("http_request: {} {}", httpMethod, trimmedUrl);

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            StringBuilder result = new StringBuilder();
            result.append("HTTP ").append(response.statusCode()).append("\n");

            String responseBody = response.body();
            if (StrUtil.isEmpty(responseBody)) {
                result.append("<无响应体>");
            } else if (responseBody.length() > MAX_RESPONSE_LENGTH) {
                result.append(responseBody, 0, MAX_RESPONSE_LENGTH);
                result.append("\n\n... 响应已截断，显示前 ").append(MAX_RESPONSE_LENGTH).append(" 字符 (共 ")
                        .append(responseBody.length()).append(" 字符)");
            } else {
                result.append(responseBody);
            }

            return result.toString();

        } catch (Exception e) {
            log.error("HTTP 请求失败: {} {}", httpMethod, trimmedUrl, e);
            return "Error: " + e.getMessage();
        }
    }

    private void validatePublicHttpTarget(URI uri) throws Exception {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("URL 必须以 http:// 或 https:// 开头");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host 不能为空");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("禁止访问本机地址");
        }

        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("禁止访问本机或内网地址: " + host);
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        byte[] raw = address.getAddress();
        boolean uniqueLocalIpv6 = raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || uniqueLocalIpv6;
    }
}
