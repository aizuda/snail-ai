package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.model.ChatModel;

/**
 * ChatModel 构建工厂。
 */
public interface ChatModelFactory {

    ChatModel build(ChatDispatchRequest.ModelConfig modelConfig);
}
