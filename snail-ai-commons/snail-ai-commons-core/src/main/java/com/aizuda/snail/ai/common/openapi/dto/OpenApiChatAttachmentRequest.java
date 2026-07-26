package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 对话附件请求。
 *
 * @author opensnail
 * @since 2026-07-09
 */
@Data
public class OpenApiChatAttachmentRequest {

    @NotNull(message = "resourceId is required")
    private Long resourceId;

    @NotBlank(message = "type is required")
    private String type;
}
