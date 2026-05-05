package com.aizuda.snail.ai.features.rag.strategy.importer;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class UrlImportStrategy implements DocumentImportStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
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
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new SnailAiException("Failed to download from URL, status: " + response.statusCode());
            }

            String fileName = extractFileName(url, request.getName());
            String fileType = extractFileType(fileName);

            byte[] fileBytes;
            try (InputStream body = response.body()) {
                fileBytes = body.readAllBytes();
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
}
