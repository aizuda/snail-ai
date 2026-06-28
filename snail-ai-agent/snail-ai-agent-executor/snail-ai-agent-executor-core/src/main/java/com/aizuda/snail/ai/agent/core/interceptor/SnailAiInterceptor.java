package com.aizuda.snail.ai.agent.core.interceptor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.core.Ordered;

/**
 * 客户端 LLM 调用拦截器 SPI。
 */
public interface SnailAiInterceptor extends Ordered {

    default ChatClientRequest beforeRequest(ChatClientRequest request) {
        return request;
    }

    default ChatClientResponse afterResponse(ChatClientResponse response) {
        return response;
    }

    @Override
    default int getOrder() {
        return 0;
    }
}
