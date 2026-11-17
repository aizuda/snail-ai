package com.aizuda.snail.ai.common.openapi.dto;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 对话模型用量。
 *
 * @author opensnail
 * @since 2026-11-16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiChatUsage {

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer cacheTokens;

    private Integer totalTokens;

    /**
     * 从 Agent completion 帧构建 OpenAPI 用量。全零帧视为旧 Agent 未上报用量。
     *
     * @param completion Agent completion 帧
     * @return 可用的模型用量；未上报时返回 {@code null}
     */
    public static OpenApiChatUsage fromCompletion(ChatStreamResponse completion) {
        if (completion == null) {
            return null;
        }
        int promptTokens = normalize(completion.getPromptTokens());
        int completionTokens = normalize(completion.getCompletionTokens());
        int cacheTokens = normalize(completion.getCacheTokens());
        if (promptTokens == 0 && completionTokens == 0 && cacheTokens == 0) {
            return null;
        }
        return OpenApiChatUsage.builder()
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .cacheTokens(cacheTokens)
                .totalTokens(safeAdd(promptTokens, completionTokens))
                .build();
    }

    private static int normalize(Integer tokens) {
        return tokens == null ? 0 : Math.max(tokens, 0);
    }

    private static int safeAdd(int first, int second) {
        long total = (long) first + second;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}
