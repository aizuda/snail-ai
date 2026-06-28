package com.aizuda.snail.ai.model.chat;

import org.springframework.ai.chat.model.ChatModel;

/**
 * ChatModelAdapter
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatModelAdapter {

    String adapterKey();

    ChatModel create(ChatModelSpec spec);
}
