package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * OpenAPI 对话请求
 */
@Data
public class OpenApiChatRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "conversationId is required")
    private String conversationId;

    @NotBlank(message = "content is required")
    private String content;

    private List<Long> disabledMcpServerIds;

    private List<Long> disabledSkillIds;

    /**
     * 提交ID，用于区分同一用户在同一对话中的并发请求。
     * 可选，不传则服务端自动生成 UUID。
     */
    private String sid;

    private long timeout;
}
