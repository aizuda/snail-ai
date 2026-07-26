package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 资源上传请求。
 *
 * @author opensnail
 * @since 2026-07-09
 */
@Data
public class OpenApiResourceUploadRequest {

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "originalName is required")
    private String originalName;

    @NotNull(message = "fileSize is required")
    private Long fileSize;

    @NotNull(message = "content is required")
    private byte[] content;

    private String bizType;

    private Long bizId;
}
