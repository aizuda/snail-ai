package com.aizuda.snail.ai.agent.core.executor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import com.aizuda.snail.ai.agent.common.context.AgentChatContextThreadLocalAccessor;
import com.aizuda.snail.ai.agent.core.advisor.ClientAdvisorKeys;
import com.aizuda.snail.ai.agent.core.advisor.ClientStreamExecutionContext;
import com.aizuda.snail.ai.agent.core.executor.client.ChatClientBuildRequest;
import com.aizuda.snail.ai.agent.core.executor.client.ChatClientFactory;
import com.aizuda.snail.ai.agent.core.executor.prompt.PromptFactory;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端 LLM 执行引擎：通过 {@link org.springframework.ai.chat.client.advisor.api.Advisor} 责任链管理
 * Prompt 注入、拦截器、思维链与 chunk 转发，自身仅负责组装 {@link ChatClient} 与流式调用。
 */
@Slf4j
public class ClientChatExecutor {

    private final PromptFactory promptFactory;
    private final ChatClientFactory chatClientFactory;

    public ClientChatExecutor(ClientChatExecutorConfig config) {
        this.promptFactory = config.getPromptFactory();
        this.chatClientFactory = config.getChatClientFactory();
    }

    /**
     * 流式执行：Advisor 链负责 memory/history、拦截、thinking 上报与 chunk 回调。
     */
    public void executeStream(ClientChatExecutionRequest request) {
        ChatDispatchRequest dispatchRequest = request.getDispatchRequest();
        try {
            ClientStreamExecutionContext state = new ClientStreamExecutionContext();
            streamResponses(request, state)
                    .contextWrite(ctx -> ctx.put(
                            AgentChatContextThreadLocalAccessor.KEY, buildContext(dispatchRequest)))
                    .doFinally(signal -> AgentChatContextHolder.clear())
                    .subscribe(
                            response -> {
                            },
                            request::acceptError,
                            () -> request.acceptCompletion(state.toCompletionResult())
                    );
        } catch (Exception e) {
            log.error("Failed to execute chat", e);
            AgentChatContextHolder.clear();
            request.acceptError(e);
        }
    }

    /**
     * 返回原始响应流（文本 chunk 已由 {@link com.aizuda.snail.ai.agent.core.advisor.StreamChunkForwarderAdvisor} 转发）。
     */
    public Flux<ChatResponse> streamResponses(ClientChatExecutionRequest request,
                                              ClientStreamExecutionContext state) {
        ChatDispatchRequest dispatchRequest = request.getDispatchRequest();
        List<ToolCallback> tools = request.getTools() != null ? request.getTools() : List.of();
        ChatClient chatClient = chatClientFactory.build(ChatClientBuildRequest.builder()
                .dispatchRequest(dispatchRequest)
                .tools(tools)
                .build());
        Prompt prompt = promptFactory.build(dispatchRequest);

        return chatClient.prompt(prompt).advisors(a -> a
                .param(ClientAdvisorKeys.DISPATCH, dispatchRequest)
                .param(ClientAdvisorKeys.STREAM_STATE, state)
                .param(ClientAdvisorKeys.CHUNK_CONSUMER, safeConsumer(request.getMessageDeltaConsumer()))
                .param(ClientAdvisorKeys.THINKING_CONSUMER, safeConsumer(request.getThinkingDeltaConsumer())))
                .stream().chatResponse();
    }

    /**
     * @deprecated use {@link #streamResponses(ClientChatExecutionRequest, ClientStreamExecutionContext)}
     */
    @Deprecated
    public Flux<ChatResponse> executeFlux(ClientChatExecutionRequest request,
                                          ClientStreamExecutionContext state) {
        return streamResponses(request, state);
    }

    private AgentChatContextHolder.ChatContext buildContext(ChatDispatchRequest req) {
        AgentChatContextHolder.ChatContext ctx = new AgentChatContextHolder.ChatContext();
        if (req.getAgentConfig() != null) {
            ctx.setAgentId(req.getAgentConfig().getAgentId());
        }
        ctx.setConversationId(req.getConversationId());
        if (req.getModelConfig() != null) {
            ctx.setModelKey(req.getModelConfig().getModelKey());
        }
        return ctx;
    }

    private Consumer<String> safeConsumer(Consumer<String> consumer) {
        return consumer != null ? consumer : ignored -> {
        };
    }

    public record ChatCompletionResult(String fullText, String fullThinking,
                                       int promptTokens, int completionTokens, int cacheTokens, long durationMs) {
    }

    @Data
    @Builder
    public static class ClientChatExecutorConfig {
        private PromptFactory promptFactory;
        private ChatClientFactory chatClientFactory;
    }
}
