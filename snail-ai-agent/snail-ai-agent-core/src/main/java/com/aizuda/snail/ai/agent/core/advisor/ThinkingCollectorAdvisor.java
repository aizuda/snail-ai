package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>
 * Spring AI M8 迁移至官方 OpenAI Java SDK 后，reasoning_content 不再写入 AssistantMessage metadata，
 * 而是存在于 {@code ChatCompletionChunk.Choice.Delta._additionalProperties()} 中。
 * 本 Advisor 兼容两种提取路径。
 */
@Slf4j
@RequiredArgsConstructor
public class ThinkingCollectorAdvisor implements StreamAdvisor {

    private static final String[] THINKING_KEYS = {"reasoningContent", "thinking", "reasoning"};

    /**
     * OpenAI 兼容 API（如 DeepSeek）在 _additionalProperties 中使用的 key
     */
    private static final String[] ADDITIONAL_PROP_KEYS = {"reasoning_content", "reasoning", "thinking"};

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

        // 1. 优先从 AssistantMessage metadata 读取（兼容 M4 及其他直接写入 metadata 的提供者）
        Map<String, Object> metadata = generation.getOutput().getMetadata();
        for (String key : THINKING_KEYS) {
            Object value = metadata.get(key);
            if (value instanceof String s && !s.isEmpty()) {
                forward(s, state, thinkingConsumer);
                return;
            }
        }

        // 2. M8 兼容：从底层 OpenAI SDK 对象的 _additionalProperties() 中提取
        String reasoning = extractFromAdditionalProperties(generation);
        if (reasoning != null && !reasoning.isEmpty()) {
            forward(reasoning, state, thinkingConsumer);
        }
    }

    /**
     * Spring AI M8 的 OpenAiChatModel 在流式场景下，会将原始 {@code ChatCompletionChunk.Choice}
     * 放入 AssistantMessage 的 metadata（key: "chunkChoice"）。
     * reasoning_content 等非标准字段存在于 {@code Delta._additionalProperties()} 中。
     */
    private String extractFromAdditionalProperties(Generation generation) {
        try {
            Map<String, Object> metadata = generation.getOutput().getMetadata();

            Object chunkChoice = metadata.get("chunkChoice");
            if (chunkChoice instanceof ChatCompletionChunk.Choice choice) {
                return extractReasoningFromAdditionalProperties(choice.delta()._additionalProperties());
            }

            Object message = metadata.get("message");
            if (message instanceof ChatCompletionMessage chatCompletionMessage) {
                return extractReasoningFromAdditionalProperties(chatCompletionMessage._additionalProperties());
            }

            Object additionalProps = metadata.get("_additionalProperties");
            if (additionalProps instanceof Map<?, ?> additionalProperties) {
                return extractReasoningFromAdditionalProperties(additionalProperties);
            }
        } catch (Exception e) {
            log.debug("Failed to extract thinking content from additionalProperties", e);
        }
        return null;
    }

    private String extractReasoningFromAdditionalProperties(Map<?, ?> additionalProps) {
        if (additionalProps == null || additionalProps.isEmpty()) {
            return null;
        }
        for (String key : ADDITIONAL_PROP_KEYS) {
            Object value = additionalProps.get(key);
            String s = extractString(value);
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String extractString(Object value) {
        if (value instanceof JsonValue jsonValue) {
            Object s = jsonValue.asString().orElse(null);
            return s instanceof String str ? str : null;
        }
        if (value instanceof String s) {
            return s;
        }
        return null;
    }

    private void forward(String text, ClientStreamExecutionContext state, Consumer<String> thinkingConsumer) {
        state.thinkingText.append(text);
        if (thinkingConsumer != null) {
            thinkingConsumer.accept(text);
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
