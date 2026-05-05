package com.aizuda.snail.ai.admin.dto;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天流事件（HTTP 推送给前端的行分隔 JSON 格式）
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatStreamEvent {

    private String type;
    private String content;

    public static ChatStreamEvent text(String content) {
        return new ChatStreamEvent(ChatStreamResponse.TYPE_TEXT, content);
    }

    public static ChatStreamEvent thinking(String content) {
        return new ChatStreamEvent(ChatStreamResponse.TYPE_THINKING, content);
    }
}
