package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 会话身份请求（Query 参数）
 */
@Data
public class OpenApiConversationIdentityRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotBlank(message = "conversationId is required")
    private String conversationId;

    @NotBlank(message = "openId is required")
    private String openId;
}
