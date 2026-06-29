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
 * @since 2026-04-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiConversationVO {

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 智能体 ID
     */
    private Long agentId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     */
    private LocalDateTime updateDt;
}
