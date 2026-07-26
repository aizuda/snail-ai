package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 更新会话标题请求
 *
 * @author opensnail
 * @since 2026-07-06
 */
@Data
public class OpenApiConversationUpdateTitleRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotBlank(message = "conversationId is required")
    private String conversationId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "title is required")
    private String title;
}
