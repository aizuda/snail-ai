package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * OpenAPI 对话服务（已简化）
 * AgentChatService 已被移除，此服务保留接口签名以保证兼容性
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiChatService {

    private static final long SYNC_CHAT_TIMEOUT_MS = 300_000L;
    private final OpenApiUserResolver openApiUserResolver;

    /**
     * 流式对话 — 返回 SseEmitter，通过 SSE 事件推送每个 text chunk
     */
    public SseEmitter chatStream(OpenApiChatRequest request) {
        throw new SnailAiException("Agent chat service has been simplified and is not available");
    }

    /**
     * 同步对话 — 阻塞等待完整响应
     */
    public OpenApiChatSyncResponse chatSync(OpenApiChatRequest request) {
        throw new SnailAiException("Agent chat service has been simplified and is not available");
    }
}
