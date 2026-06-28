package com.aizuda.snail.ai.agent.core.executor.client;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;

/**
 * ChatClient 自定义上下文。
 */
@Data
@Builder
public class ChatClientBuildContext {

    private ChatClientBuildRequest request;
    private ChatClient.Builder builder;
}
