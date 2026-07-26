package com.aizuda.snail.ai.agent.core.executor.prompt;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Prompt 构建工厂。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface PromptFactory {

    /**
     * 根据对话分发请求构建 {@link Prompt}（系统提示 + 历史消息 + 用户消息）。
     *
     * @param request 对话分发请求
     * @return 构建完成的 Prompt
     */
    Prompt build(ChatDispatchRequest request);
}
