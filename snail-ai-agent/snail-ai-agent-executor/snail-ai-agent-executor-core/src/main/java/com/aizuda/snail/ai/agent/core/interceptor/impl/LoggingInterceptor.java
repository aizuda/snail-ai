package com.aizuda.snail.ai.agent.core.interceptor.impl;

import com.aizuda.snail.ai.agent.core.interceptor.SnailAiInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.Optional;

/**
 * 可选的请求/响应日志拦截器。
 */
@Slf4j
public class LoggingInterceptor implements SnailAiInterceptor {

    @Override
    public ChatClientRequest beforeRequest(ChatClientRequest request) {
        int n = request.prompt() != null && request.prompt().getInstructions() != null
                ? request.prompt().getInstructions().size() : 0;
        log.info("LLM request: {} messages", n);
        return request;
    }

    @Override
    public ChatClientResponse afterResponse(ChatClientResponse response) {
        String finish = Optional.ofNullable(response)
                .map(ChatClientResponse::chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getMetadata)
                .map(m -> String.valueOf(m.getFinishReason()))
                .orElse("unknown");
        log.info("LLM response: finishReason={}", finish);
        return response;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
