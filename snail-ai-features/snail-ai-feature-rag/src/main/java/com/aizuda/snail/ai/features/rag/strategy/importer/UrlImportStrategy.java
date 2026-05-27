package com.aizuda.snail.ai.features.rag.strategy.importer;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Slf4j
@Component
public class UrlImportStrategy implements DocumentImportStrategy {

    private static final int MAX_URL_IMPORT_BYTES = 50 * 1024 * 1024;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public boolean supports(String sourceType) {
        return DocumentSourceTypeEnum.URL.getValue().equalsIgnoreCase(sourceType);
    }

    @Override
    public ImportResult importDocument(ImportRequest request) {
        String url = request.getUrl();
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("URL is required for URL import");
        }

        try {
            URI uri = URI.create(url);
            validatePublicHttpTarget(uri);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new SnailAiException("Failed to download from URL, status: " + response.statusCode());
            }
            validateContentLength(response);

            String fileName = extractFileName(url, request.getName());
            String fileType = extractFileType(fileName);

            byte[] fileBytes;
            try (InputStream body = response.body()) {
                fileBytes = readLimited(body);
            }

            String contentHash = ContentHashUtil.sha256Hex(fileBytes);

            return ImportResult.builder()
                    .fileName(fileName)
                    .fileType(fileType)
                    .sourceType(DocumentSourceTypeEnum.URL.getValue())
                    .contentHash(contentHash)
                    .rawBytes(fileBytes)
                    .fileSize(fileBytes.length)
                    .build();

        } catch (Exception e) {
            throw new SnailAiException("Failed to import from URL: " + url, e);
        }
    }

    private String extractFileName(String url, String fallbackName) {
        if (StrUtil.isNotBlank(fallbackName)) {
            return fallbackName;
        }
        String path = URI.create(url).getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return "url-document.html";
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "html";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateContentLength(HttpResponse<?> response) {
        response.headers().firstValueAsLong("Content-Length")
                .ifPresent(length -> {
                    if (length > MAX_URL_IMPORT_BYTES) {
                        throw new SnailAiException("URL document is too large: " + length);
                    }
                });
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_URL_IMPORT_BYTES) {
                throw new SnailAiException("URL document exceeds max size: " + MAX_URL_IMPORT_BYTES);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void validatePublicHttpTarget(URI uri) throws Exception {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is required");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("Localhost URL import is not allowed");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("Private network URL import is not allowed: " + host);
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
