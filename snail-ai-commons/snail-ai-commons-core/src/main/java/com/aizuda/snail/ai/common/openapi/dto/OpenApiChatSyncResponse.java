package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 同步对话响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiChatSyncResponse {

    private String conversationId;

    private String content;

    private Long durationMs;
}
