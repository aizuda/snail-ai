package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpenAPI 资源响应。
 *
 * @author opensnail
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiResourceResponse {

    private Long id;
    private String storageKey;
    private String originalName;
    private Long fileSize;
    private String mimeType;
    private String storageType;
    private String accessUrl;
    private String bizType;
    private Long bizId;
    private Long creatorId;
    private LocalDateTime createDt;
}
