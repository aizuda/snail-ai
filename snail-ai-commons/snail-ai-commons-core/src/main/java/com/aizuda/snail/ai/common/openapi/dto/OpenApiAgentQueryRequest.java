package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * OpenAPI Agent 分页查询请求。
 */
@Data
public class OpenApiAgentQueryRequest {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "size must be greater than 0")
    private Integer size = 50;
}
