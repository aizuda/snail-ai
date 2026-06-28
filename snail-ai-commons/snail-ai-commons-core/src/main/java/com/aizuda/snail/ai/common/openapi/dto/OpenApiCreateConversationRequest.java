package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 创建会话请求
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
public class OpenApiCreateConversationRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "title is required")
    private String title;
}
