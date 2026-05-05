package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 累积流式思维链 metadata，完成时桥接到 {@link AgentChatContextHolder.ChatContext}
 * 供 GENERATION 观测的 {@code ThinkingContentExtractor} 读取。
 * <p>
 * 同时将完整 thinkingText 保留在 {@link ClientStreamExecutionContext}，
 * 随 {@code ChatCompletionResult.fullThinking} 返回给调用方用于对话记录持久化。
 */
@RequiredArgsConstructor
public class ThinkingCollectorAdvisor implements StreamAdvisor {

    private static final String[] THINKING_KEYS = {"reasoningContent", "thinking", "reasoning"};

    @Override
    public String getName() {
        return "ThinkingCollectorAdvisor";
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Object st = request.context().get(ClientAdvisorKeys.STREAM_STATE);
        if (!(st instanceof ClientStreamExecutionContext state)) {
            return chain.nextStream(request);
        }

        @SuppressWarnings("unchecked")
        Consumer<String> thinkingConsumer = (Consumer<String>) request.context().get(ClientAdvisorKeys.THINKING_CONSUMER);

        return chain.nextStream(request)
                .doOnNext(response -> extractThinking(response, state, thinkingConsumer))
                .doOnComplete(() -> bridgeThinkingToContext(state));
    }

    private void extractThinking(ChatClientResponse response, ClientStreamExecutionContext state,
                                 Consumer<String> thinkingConsumer) {
        ChatResponse cr = response.chatResponse();
        if (cr == null || cr.getResult() == null) {
            return;
        }
        Generation generation = cr.getResult();
        Map<String, Object> metadata = generation.getOutput().getMetadata();
        for (String key : THINKING_KEYS) {
            Object value = metadata.get(key);
            if (value instanceof String s && !s.isEmpty()) {
                state.thinkingText.append(s);
                if (thinkingConsumer != null) {
                    thinkingConsumer.accept(s);
                }
                return;
            }
        }
    }

    /**
     * 流完成时将累积的思维链桥接到 ChatContext，供 ObservationHandler 的提取器读取
     */
    private void bridgeThinkingToContext(ClientStreamExecutionContext state) {
        String thinking = state.thinkingText.toString();
        if (thinking.isEmpty()) {
            return;
        }
        AgentChatContextHolder.ChatContext ctx = AgentChatContextHolder.getContext();
        if (ctx != null) {
            ctx.setCurrentThinkingContent(thinking);
        }
    }
}
