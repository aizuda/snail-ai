package com.aizuda.snail.ai.model.chat;

import org.springframework.ai.chat.model.ChatModel;

public interface ChatModelAdapter {

    String adapterKey();

    ChatModel create(ChatModelSpec spec);
}
