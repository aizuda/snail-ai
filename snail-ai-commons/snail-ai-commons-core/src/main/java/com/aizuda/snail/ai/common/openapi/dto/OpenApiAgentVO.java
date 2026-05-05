package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI Agent 概要信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiAgentVO {

    private Long id;

    private String name;

    private String description;

    private String avatar;

    private String greeting;

    private Integer status;
}
