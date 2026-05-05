package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 dispatch 中的 memory_context 与 history_messages 注入 Prompt。
 */
public class MemoryInjectionAdvisor implements BaseAdvisor {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    @Override
    public String getName() {
        return "MemoryInjectionAdvisor";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Object d = request.context().get(ClientAdvisorKeys.DISPATCH);
        if (!(d instanceof ChatDispatchRequest dispatch)) {
            return request;
        }
        List<Message> messages = new ArrayList<>();
        addSystemMessage(messages, dispatch);
        addHistoryMessages(messages, dispatch.getHistoryMessages());
        addUserMessage(messages, dispatch.getUserMessage());
        Prompt p = new Prompt(messages, request.prompt().getOptions());
        return new ChatClientRequest(p, request.context());
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    private void addSystemMessage(List<Message> messages, ChatDispatchRequest request) {
        String systemPrompt = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
        String memoryContext = request.getMemoryContext() != null ? request.getMemoryContext() : "";
        if (!memoryContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + memoryContext;
        }
        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
    }

    private void addHistoryMessages(List<Message> messages, List<ChatDispatchRequest.HistoryMessage> historyMessages) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return;
        }
        for (ChatDispatchRequest.HistoryMessage msg : historyMessages) {
            if (ROLE_USER.equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (ROLE_ASSISTANT.equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
    }

    private void addUserMessage(List<Message> messages, String userMessage) {
        if (userMessage != null && !userMessage.isEmpty()) {
            messages.add(new UserMessage(userMessage));
        }
    }
}
