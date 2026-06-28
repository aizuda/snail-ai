package com.aizuda.snail.ai.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpenAPI 会话信息
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiConversationVO {

    private String conversationId;

    private Long agentId;

    private String title;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
