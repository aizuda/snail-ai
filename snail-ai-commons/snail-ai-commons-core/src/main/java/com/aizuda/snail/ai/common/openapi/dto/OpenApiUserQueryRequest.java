package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OpenAPI 用户查询请求
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
public class OpenApiUserQueryRequest {

    @NotBlank(message = "openId is required")
    private String openId;
}
