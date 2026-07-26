package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.model.ChatModel;

/**
 * ChatModel 构建工厂。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatModelFactory {

    /**
     * 根据模型配置构建 {@link ChatModel} 实例。
     *
     * @param modelConfig 对话模型配置
     * @return 构建完成的 ChatModel
     */
    ChatModel build(ChatDispatchRequest.ModelConfig modelConfig);
}
