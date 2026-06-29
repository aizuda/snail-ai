package com.aizuda.snail.ai.admin.vo;

import lombok.Data;

/**
 * 会话列表响应：会话 ID 与标题。
 *
 * @author opensnail
 * @since 2025-07-13
 */
@Data
public class ConversationResponseVO {

    private Long id;

    private String conversationId;

    private String title;
}
