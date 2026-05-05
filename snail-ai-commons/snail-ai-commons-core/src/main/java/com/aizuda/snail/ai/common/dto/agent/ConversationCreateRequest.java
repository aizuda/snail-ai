package com.aizuda.snail.ai.common.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建对话请求
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCreateRequest {

    private Long agentId;
    private Long userId;
    private String conversationId;
    private String userMessage;
}
