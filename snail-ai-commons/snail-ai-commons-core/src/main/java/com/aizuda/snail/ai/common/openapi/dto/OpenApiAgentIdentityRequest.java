package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OpenAPI Agent 标识请求
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
public class OpenApiAgentIdentityRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;
}
