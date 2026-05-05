package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 会话信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiConversationVO {

    private String conversationId;

    private Long agentId;

    private String title;

    private String createDt;

    private String updateDt;
}
