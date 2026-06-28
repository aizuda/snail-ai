package com.aizuda.snail.ai.agent.core.executor.prompt;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Prompt 构建工厂。
 */
public interface PromptFactory {

    Prompt build(ChatDispatchRequest request);
}
