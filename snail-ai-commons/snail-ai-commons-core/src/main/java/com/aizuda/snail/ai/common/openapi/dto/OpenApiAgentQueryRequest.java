package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * OpenAPI Agent 分页查询请求。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class OpenApiAgentQueryRequest {

    @Min(value = 1, message = "page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "size must be greater than 0")
    private Integer size = 50;

    /** 应用 ID 过滤（内部参数，非必填）；为空时不过滤，返回所有已上架智能体 */
    private String appId;
}
