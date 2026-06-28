package com.aizuda.snail.ai.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.security.annotation.OriginalControllerReturnValue;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceStorageTypeEnum;
import com.aizuda.snail.ai.features.resource.strategy.ResourceStorageFactory;
import com.aizuda.snail.ai.features.resource.strategy.ResourceStorageService;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 公开资源访问入口。访问凭证是包含 UUID 的 storageKey，不使用自增资源 ID。
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileAccessController {

    private static final String FILES_PREFIX = "/files/";
    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_VALUE_NOSNIFF = "nosniff";
    private static final String DEFAULT_FILE_NAME = "resource";
    private static final String MIME_IMAGE_WEBP = "image/webp";
    private static final String MIME_TEXT_CSV = "text/csv";
    private static final String MIME_TEXT_MARKDOWN = "text/markdown";
    private static final String MIME_TEXT_X_MARKDOWN = "text/x-markdown";
    private static final long PUBLIC_CACHE_MAX_AGE_SECONDS = 86400L;
    private static final Pattern STORAGE_KEY_PATTERN = Pattern.compile(
            "^[a-z0-9][a-z0-9_-]*/\\d{4}/\\d{2}/\\d{2}/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                    + "(\\.[a-z0-9]+)?$");
    private static final Set<String> INLINE_MIME_TYPES = Set.of(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            MIME_IMAGE_WEBP,
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MIME_TEXT_CSV,
            MIME_TEXT_MARKDOWN,
            MIME_TEXT_X_MARKDOWN
    );

    private final ResourceService resourceService;
    private final ResourceStorageFactory storageFactory;

    @GetMapping("/**")
    @OriginalControllerReturnValue
    public ResponseEntity<InputStreamResource> access(HttpServletRequest request,
                                                       @RequestParam(value = "download", required = false)
                                                       Boolean download) {
        String storageKey = extractStorageKey(request);
        ResourcePO resource = resourceService.getByStorageKey(storageKey);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        if (!isProxyStorage(resource.getStorageType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        ResourceStorageService storage = storageFactory.get(resource.getStorageType());
        InputStream inputStream = storage.load(storageKey);
        MediaType mediaType = parseMediaType(resource.getMimeType());
        boolean attachment = Boolean.TRUE.equals(download) || !isInlineAllowed(mediaType);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(PUBLIC_CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .header(HEADER_X_CONTENT_TYPE_OPTIONS, HEADER_VALUE_NOSNIFF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(resource.getOriginalName(), attachment));

        if (resource.getFileSize() != null && resource.getFileSize() >= 0) {
            builder.contentLength(resource.getFileSize());
        }

        return builder.body(new InputStreamResource(inputStream));
    }

    private String extractStorageKey(HttpServletRequest request) {
        String requestUri = decodeRequestUri(request);
        String contextPath = StrUtil.nullToEmpty(request.getContextPath());
        String prefix = contextPath + FILES_PREFIX;
        if (!requestUri.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        String storageKey = requestUri.substring(prefix.length());
        if (!isValidStorageKey(storageKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return storageKey;
    }

    private String decodeRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    private boolean isValidStorageKey(String storageKey) {
        return StrUtil.isNotBlank(storageKey)
                && STORAGE_KEY_PATTERN.matcher(storageKey).matches();
    }

    private boolean isProxyStorage(String storageType) {
        ResourceStorageTypeEnum type = ResourceStorageTypeEnum.fromValue(storageType);
        return Objects.equals(type, ResourceStorageTypeEnum.LOCAL)
                || Objects.equals(type, ResourceStorageTypeEnum.MINIO);
    }

    private MediaType parseMediaType(String mimeType) {
        if (StrUtil.isBlank(mimeType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean isInlineAllowed(MediaType mediaType) {
        String value = mediaType.getType().toLowerCase(Locale.ROOT)
                + "/" + mediaType.getSubtype().toLowerCase(Locale.ROOT);
        return INLINE_MIME_TYPES.contains(value);
    }

    private String contentDisposition(String fileName, boolean attachment) {
        ContentDisposition.Builder builder = attachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline();
        return builder.filename(StrUtil.blankToDefault(fileName, DEFAULT_FILE_NAME), StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
