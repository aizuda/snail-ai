package com.aizuda.snail.ai.model.chat;

import org.springframework.ai.chat.model.ChatModel;

/**
 * 对话模型适配器接口，按规格创建对应供应商的 {@link ChatModel}。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatModelAdapter {

    /**
     * 适配器唯一标识，用于与模型配置的 adapterKey 匹配
     *
     * @return 适配器键
     */
    String adapterKey();

    /**
     * 按规格创建对话模型
     *
     * @param spec 对话模型构建规格
     * @return 对话模型实例
     */
    ChatModel create(ChatModelSpec spec);
}
