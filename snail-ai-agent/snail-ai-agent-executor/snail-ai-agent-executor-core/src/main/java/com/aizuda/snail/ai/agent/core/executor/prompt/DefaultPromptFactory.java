package com.aizuda.snail.ai.agent.core.executor.prompt;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 Prompt 工厂。
 */
public class DefaultPromptFactory implements PromptFactory {

    @Override
    public Prompt build(ChatDispatchRequest request) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (request.getUserMessage() != null && !request.getUserMessage().isEmpty()) {
            messages.add(new UserMessage(request.getUserMessage()));
        }
        return new Prompt(messages);
    }
}
