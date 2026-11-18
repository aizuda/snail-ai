package com.aizuda.snail.ai.admin.dto;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天流事件（HTTP 推送给前端的行分隔 JSON 格式）
 *
 * @author opensnail
 * @since 2025-04-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatStreamEvent {

    private String type;
    private String content;
    private ChatUsage usage;

    public static ChatStreamEvent text(String content) {
        return new ChatStreamEvent(ChatStreamResponse.TYPE_TEXT, content, null);
    }

    public static ChatStreamEvent thinking(String content) {
        return new ChatStreamEvent(ChatStreamResponse.TYPE_THINKING, content, null);
    }

    public static ChatStreamEvent completion(ChatStreamResponse completion) {
        return new ChatStreamEvent(ChatStreamResponse.TYPE_COMPLETION, null, ChatUsage.fromCompletion(completion));
    }

    /** 管理端对话完成时下发的 Token 用量；全零帧视为旧 Agent 未上报。 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatUsage {

        private static final int MAX_TOKEN_COUNT = Integer.MAX_VALUE;

        private Integer promptTokens;
        private Integer completionTokens;
        private Integer cacheTokens;
        private Integer totalTokens;

        private static ChatUsage fromCompletion(ChatStreamResponse completion) {
            if (completion == null) {
                return null;
            }
            int promptTokens = normalize(completion.getPromptTokens());
            int completionTokens = normalize(completion.getCompletionTokens());
            int cacheTokens = normalize(completion.getCacheTokens());
            if (promptTokens == 0 && completionTokens == 0 && cacheTokens == 0) {
                return null;
            }
            return new ChatUsage(
                    promptTokens,
                    completionTokens,
                    cacheTokens,
                    safeAdd(promptTokens, completionTokens));
        }

        private static int normalize(Integer tokens) {
            return tokens == null ? 0 : Math.max(tokens, 0);
        }

        private static int safeAdd(int first, int second) {
            long total = (long) first + second;
            return total > MAX_TOKEN_COUNT ? MAX_TOKEN_COUNT : (int) total;
        }
    }
}
