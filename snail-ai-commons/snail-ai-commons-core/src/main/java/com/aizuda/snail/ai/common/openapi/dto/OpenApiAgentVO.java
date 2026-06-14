package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    private List<String> presetQuestions;

    private Boolean mcpEnabled;

    private List<OpenApiAgentToolVO> mcpServers;

    private Boolean skillEnabled;

    private List<OpenApiAgentToolVO> skills;

    private Boolean webSearchEnabled;

    private Integer viewCount;

    private Boolean isFeatured;

    private Integer status;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;

    private Boolean subscribed;
}
