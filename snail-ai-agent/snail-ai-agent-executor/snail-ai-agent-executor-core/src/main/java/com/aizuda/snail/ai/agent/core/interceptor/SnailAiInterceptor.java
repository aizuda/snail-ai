package com.aizuda.snail.ai.agent.core.interceptor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.core.Ordered;

/**
 * 客户端 LLM 调用拦截器 SPI。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface SnailAiInterceptor extends Ordered {

    /**
     * 请求发往 LLM 前的回调，可读取或改写请求。
     *
     * @param request 原始 ChatClient 请求
     * @return 处理后的请求（默认原样返回）
     */
    default ChatClientRequest beforeRequest(ChatClientRequest request) {
        return request;
    }

    /**
     * LLM 响应返回后的回调，可读取或改写响应。
     *
     * @param response 原始 ChatClient 响应
     * @return 处理后的响应（默认原样返回）
     */
    default ChatClientResponse afterResponse(ChatClientResponse response) {
        return response;
    }

    @Override
    default int getOrder() {
        return 0;
    }
}
