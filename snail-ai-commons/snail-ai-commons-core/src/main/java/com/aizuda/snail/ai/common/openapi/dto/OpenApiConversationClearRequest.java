package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 会话清空请求（Query 参数）
 */
@Data
public class OpenApiConversationClearRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotBlank(message = "openId is required")
    private String openId;
}
