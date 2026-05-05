package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI Agent 标识请求
 */
@Data
public class OpenApiAgentIdentityRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;
}
