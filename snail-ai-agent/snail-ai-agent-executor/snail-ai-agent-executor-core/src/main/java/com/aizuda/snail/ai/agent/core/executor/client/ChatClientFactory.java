package com.aizuda.snail.ai.agent.core.executor.client;

import org.springframework.ai.chat.client.ChatClient;

/**
 * ChatClient 构建工厂。
 */
public interface ChatClientFactory {

    ChatClient build(ChatClientBuildRequest request);
}
