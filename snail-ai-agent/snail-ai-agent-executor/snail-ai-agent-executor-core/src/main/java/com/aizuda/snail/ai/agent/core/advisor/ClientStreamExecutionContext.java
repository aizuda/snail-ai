package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次流式调用的累积状态（由 Advisor 写入，完成时转为 {@link ClientChatExecutor.ChatCompletionResult}）。
 */
public class ClientStreamExecutionContext {

    public final StringBuilder fullText = new StringBuilder();
    public final StringBuilder thinkingText = new StringBuilder();
    public final long startTime = System.currentTimeMillis();

    /** 累积的工具调用列表 (stream 模式下逐步收集) */
    private final List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

    /** 流式最终 chunk 中的 Token 使用量 */
    @Setter
    private int promptTokens;
    @Setter
    private int completionTokens;

    public void addToolCall(AssistantMessage.ToolCall toolCall) {
        if (toolCall != null && !containsToolCall(toolCall.id())) {
            toolCalls.add(toolCall);
        }
    }

    public void addToolCalls(List<AssistantMessage.ToolCall> calls) {
        if (calls != null) {
            calls.forEach(this::addToolCall);
        }
    }

    public List<AssistantMessage.ToolCall> getToolCalls() {
        return new ArrayList<>(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    private boolean containsToolCall(String id) {
        return toolCalls.stream().anyMatch(tc -> tc.id().equals(id));
    }

    public ClientChatExecutor.ChatCompletionResult toCompletionResult() {
        long duration = System.currentTimeMillis() - startTime;
        return new ClientChatExecutor.ChatCompletionResult(
                fullText.toString(), thinkingText.toString(), promptTokens, completionTokens, duration);
    }
}
