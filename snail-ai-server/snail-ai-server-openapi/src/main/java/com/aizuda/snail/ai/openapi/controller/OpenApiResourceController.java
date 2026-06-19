package com.aizuda.snail.ai.openapi.controller;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * OpenAPI 资源预览接口，供嵌入式聊天网关以服务端身份拉取受保护图片。
 */
@RestController
@RequiredArgsConstructor
public class OpenApiResourceController {

    private static final String IMAGE_MIME_TYPE_PREFIX = "image/";
    private static final String UTF_8_FILENAME_PREFIX = "inline; filename*=UTF-8''";

    private final ResourceService resourceService;

    @GetMapping(OpenApiPathConstants.OPEN_API_RESOURCE_PREVIEW)
    public ResponseEntity<InputStreamResource> preview(@PathVariable("id") Long id) {
        ResourcePO resource = resourceService.requireResource(id);
        validateImageResource(resource);
        InputStream inputStream = resourceService.load(id);
        String encodedName = URLEncoder.encode(resource.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, UTF_8_FILENAME_PREFIX + encodedName)
                .body(new InputStreamResource(inputStream));
    }

    private void validateImageResource(ResourcePO resource) {
        String mimeType = resource.getMimeType();
        if (StrUtil.isBlank(mimeType)
                || !mimeType.toLowerCase(Locale.ROOT).startsWith(IMAGE_MIME_TYPE_PREFIX)) {
            throw new SnailAiException("Only image resources can be previewed: {}", resource.getId());
        }
    }
}
