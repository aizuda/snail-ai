package com.aizuda.snail.ai.common.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天流式响应
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamResponse {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_THINKING = "thinking";
    public static final String TYPE_COMPLETION = "completion";
    public static final String TYPE_ERROR = "error";

    /** 响应类型 */
    private String type;

    /** 提交ID，用于区分同一用户在同一对话中的并发请求 */
    private String sid;

    /** 文本内容 */
    private String content;

    /** 完整文本 */
    private String fullText;

    /** 完整思考过程 */
    private String fullThinking;

    /** 提示词令牌数 */
    private Integer promptTokens;

    /** 完成令牌数 */
    private Integer completionTokens;

    /** 执行时长（毫秒） */
    private Long durationMs;

    /** 错误代码 */
    private String errorCode;

    /** 错误消息 */
    private String errorMessage;

    public static ChatStreamResponse text(String text) {
        return ChatStreamResponse.builder()
                .type(TYPE_TEXT)
                .content(text)
                .build();
    }

    public static ChatStreamResponse text(String sid, String text) {
        return ChatStreamResponse.builder()
                .type(TYPE_TEXT)
                .sid(sid)
                .content(text)
                .build();
    }

    public static ChatStreamResponse thinking(String text) {
        return ChatStreamResponse.builder()
                .type(TYPE_THINKING)
                .content(text)
                .build();
    }

    public static ChatStreamResponse thinking(String sid, String text) {
        return ChatStreamResponse.builder()
                .type(TYPE_THINKING)
                .sid(sid)
                .content(text)
                .build();
    }

    public static ChatStreamResponse completion(String fullText, String fullThinking,
                                                 int promptTokens, int completionTokens, long durationMs) {
        return ChatStreamResponse.builder()
                .type(TYPE_COMPLETION)
                .fullText(fullText)
                .fullThinking(fullThinking)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .durationMs(durationMs)
                .build();
    }

    public static ChatStreamResponse completion(String sid, String fullText, String fullThinking,
                                                 int promptTokens, int completionTokens, long durationMs) {
        return ChatStreamResponse.builder()
                .type(TYPE_COMPLETION)
                .sid(sid)
                .fullText(fullText)
                .fullThinking(fullThinking)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .durationMs(durationMs)
                .build();
    }

    public static ChatStreamResponse error(String errorCode, String errorMessage) {
        return ChatStreamResponse.builder()
                .type(TYPE_ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public static ChatStreamResponse error(String sid, String errorCode, String errorMessage) {
        return ChatStreamResponse.builder()
                .type(TYPE_ERROR)
                .sid(sid)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
