package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI 用户智能体关联请求。
 */
@Data
public class OpenApiUserAgentRequest {

    @NotBlank(message = "openId is required")
    private String openId;

    @NotNull(message = "agentId is required")
    private Long agentId;
}
