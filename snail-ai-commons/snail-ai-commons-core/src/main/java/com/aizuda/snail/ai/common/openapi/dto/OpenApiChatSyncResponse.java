package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * OpenAPI 同步对话响应
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiChatSyncResponse {

    private String conversationId;

    private String content;

    private Long durationMs;

    /** 模型 Token 用量；模型未上报时显式返回 null */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private OpenApiChatUsage usage;
}
