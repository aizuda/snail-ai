package com.aizuda.snail.ai.agent.core.executor.client;

import org.springframework.ai.chat.client.ChatClient;

/**
 * ChatClient 构建工厂。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatClientFactory {

    /**
     * 根据构建请求创建 {@link ChatClient} 实例。
     *
     * @param request ChatClient 构建请求（含模型配置、工具等）
     * @return 构建完成的 ChatClient
     */
    ChatClient build(ChatClientBuildRequest request);
}
