package com.aizuda.snail.ai.agent.core.executor.model;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

/**
 * 客户端 Chat 模型输入。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public record ClientChatModelInput(
        ChatDispatchRequest.ModelConfig modelConfig) {
}
