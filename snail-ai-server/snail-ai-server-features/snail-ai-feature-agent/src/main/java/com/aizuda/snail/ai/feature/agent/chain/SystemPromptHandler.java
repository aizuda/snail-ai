package com.aizuda.snail.ai.feature.agent.chain;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统提示词初始化：以智能体 instruction 作为 systemPrompt 基础
 */
@Component
@Order(50)
public class SystemPromptHandler implements AgentChatHandler {

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }
        String instruction = ctx.getAgent().getInstruction();
        ctx.setSystemPrompt(instruction != null ? instruction : "You are a helpful AI assistant.");
    }
}
