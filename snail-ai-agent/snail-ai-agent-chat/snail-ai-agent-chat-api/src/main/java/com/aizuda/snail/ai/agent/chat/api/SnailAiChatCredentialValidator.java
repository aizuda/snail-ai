package com.aizuda.snail.ai.agent.chat.api;

/**
 * 嵌入式 chat 用户凭证校验扩展点。
 */
public interface SnailAiChatCredentialValidator {

    void validate(SnailAiChatSession session);
}
