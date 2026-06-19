package com.aizuda.snail.ai.agent.chat.starter;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OpenApiResourceProxy {

    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final String HEADER_APP_ID = "Snail-Ai-App-Id";
    private static final String HEADER_TOKEN = "Snail-Ai-Token";
    private static final String ACCEPT_IMAGE = "image/*";
    private static final String PATH_PARAM_RESOURCE_ID = "{id}";
    private static final Set<String> RESPONSE_HEADERS = Set.of(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaders.CACHE_CONTROL
    );

    private final HttpClient snailAiOpenApiHttpClient;
    private final SnailAiOpenApiProperties openApiProperties;
    private final SnailAiAgentProperties agentProperties;

    public ResponseEntity<byte[]> preview(Long resourceId) {
        String path = OpenApiPathConstants.OPEN_API_RESOURCE_PREVIEW
                .replace(PATH_PARAM_RESOURCE_ID, String.valueOf(resourceId));
        HttpRequest request = baseRequest(buildUrl(path))
                .header(HttpHeaders.ACCEPT, ACCEPT_IMAGE)
                .GET()
                .build();

        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() >= 400) {
            throw new SnailAiException("OpenAPI resource preview failed: HTTP {} - {}",
                    response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
        }
        return ResponseEntity.status(response.statusCode())
                .headers(responseHeaders(response))
                .body(response.body());
    }

    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(openApiProperties.getReadTimeoutMs()))
                .header(HEADER_APP_ID, agentProperties.getAppId())
                .header(HEADER_TOKEN, agentProperties.getToken());
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        try {
            return snailAiOpenApiHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SnailAiException("OpenAPI resource preview interrupted", e);
        } catch (IOException e) {
            throw new SnailAiException("OpenAPI resource preview request failed", e);
        }
    }

    private HttpHeaders responseHeaders(HttpResponse<byte[]> response) {
        HttpHeaders headers = new HttpHeaders();
        RESPONSE_HEADERS.forEach(headerName ->
                response.headers().firstValue(headerName).ifPresent(value -> headers.set(headerName, value)));
        return headers;
    }

    private String buildUrl(String path) {
        return buildBaseUrl() + (path.startsWith("/") ? path : "/" + path);
    }

    private String buildBaseUrl() {
        String host = openApiProperties.getServerHost();
        if (host == null || host.trim().isEmpty()) {
            host = agentProperties.getServer().getHost();
        }

        String protocol = openApiProperties.isHttps() ? HTTPS_PROTOCOL : HTTP_PROTOCOL;
        int port = openApiProperties.getWebPort();
        String prefix = openApiProperties.getPrefix();
        StringBuilder url = new StringBuilder(protocol).append("://").append(host.replaceAll("/$", ""));

        if ((openApiProperties.isHttps() && port != HTTPS_DEFAULT_PORT)
                || (!openApiProperties.isHttps() && port != HTTP_DEFAULT_PORT)) {
            url.append(":").append(port);
        }

        if (prefix != null && !prefix.trim().isEmpty()) {
            if (!prefix.startsWith("/")) {
                url.append("/");
            }
            url.append(prefix);
        }
        return url.toString();
    }
}
