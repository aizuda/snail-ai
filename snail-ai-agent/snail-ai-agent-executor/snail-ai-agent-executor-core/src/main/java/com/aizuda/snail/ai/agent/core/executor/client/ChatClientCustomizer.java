package com.aizuda.snail.ai.agent.core.executor.client;

import org.springframework.core.Ordered;

/**
 * ChatClient 横向自定义扩展。
 */
public interface ChatClientCustomizer {

    default String name() {
        return getClass().getName();
    }

    default int order() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    void customize(ChatClientBuildContext context);
}
