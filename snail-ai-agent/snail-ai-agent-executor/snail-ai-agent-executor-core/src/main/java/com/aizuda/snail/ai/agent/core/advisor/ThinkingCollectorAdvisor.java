package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
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
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 累积流式思维链 metadata，完成时桥接到 {@link AgentChatContextHolder.ChatContext}
 * 供 GENERATION 观测的 {@code ThinkingContentExtractor} 读取。
 * <p>
 * 同时将完整 thinkingText 保留在 {@link ClientStreamExecutionContext}，
 * 随 {@code ChatCompletionResult.fullThinking} 返回给调用方用于对话记录持久化。
 * <p>
 * 兼容 reasoning_content 直接写入 metadata，或存在于底层响应对象 _additionalProperties() 中的场景。
 */
@Slf4j
@RequiredArgsConstructor
public class ThinkingCollectorAdvisor implements StreamAdvisor {

    private static final String METADATA_REASONING_CONTENT = "reasoningContent";
    private static final String METADATA_THINKING = "thinking";
    private static final String METADATA_REASONING = "reasoning";
    private static final String[] THINKING_KEYS = {METADATA_REASONING_CONTENT, METADATA_THINKING, METADATA_REASONING};
    private static final String METADATA_CHUNK_CHOICE = "chunkChoice";
    private static final String METADATA_MESSAGE = "message";
    private static final String METADATA_ADDITIONAL_PROPERTIES = "_additionalProperties";
    private static final String METHOD_DELTA = "delta";
    private static final String METHOD_ADDITIONAL_PROPERTIES = "_additionalProperties";
    private static final String METHOD_AS_STRING = "asString";

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
        return AdvisorOrder.THINKING_COLLECTOR;
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
                .doOnNext(response -> collectThinking(response, state, thinkingConsumer))
                .doOnComplete(() -> bridgeThinkingToContext(state));
    }

    private void collectThinking(ChatClientResponse response, ClientStreamExecutionContext state,
                                 Consumer<String> thinkingConsumer) {
        ChatResponse cr = response.chatResponse();
        if (cr == null || cr.getResult() == null) {
            return;
        }
        extractThinkingText(cr.getResult()).ifPresent(text -> forward(text, state, thinkingConsumer));
    }

    private Optional<String> extractThinkingText(Generation generation) {
        return extractFromOutputMetadata(generation)
                .or(() -> extractFromAdditionalProperties(generation));
    }

    private Optional<String> extractFromOutputMetadata(Generation generation) {
        if (generation == null || generation.getOutput() == null) {
            return Optional.empty();
        }
        Map<String, Object> metadata = generation.getOutput().getMetadata();
        for (String key : THINKING_KEYS) {
            String s = extractString(metadata.get(key));
            if (s != null && !s.isBlank()) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * 不直接依赖具体模型 SDK 类型，避免 agent core 绑定某个模型厂商。
     */
    private Optional<String> extractFromAdditionalProperties(Generation generation) {
        try {
            Map<String, Object> metadata = generation.getOutput().getMetadata();

            Optional<String> chunkChoiceReasoning = extractReasoningFromCandidate(metadata.get(METADATA_CHUNK_CHOICE));
            if (chunkChoiceReasoning.isPresent()) {
                return chunkChoiceReasoning;
            }

            Optional<String> messageReasoning = extractReasoningFromCandidate(metadata.get(METADATA_MESSAGE));
            if (messageReasoning.isPresent()) {
                return messageReasoning;
            }

            Object additionalProps = metadata.get(METADATA_ADDITIONAL_PROPERTIES);
            if (additionalProps instanceof Map<?, ?> additionalProperties) {
                return extractReasoningFromAdditionalProperties(additionalProperties);
            }
        } catch (Exception e) {
            log.debug("Failed to extract thinking content from additionalProperties", e);
        }
        return Optional.empty();
    }

    private Optional<String> extractReasoningFromCandidate(Object candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        if (candidate instanceof Map<?, ?> additionalProperties) {
            return extractReasoningFromAdditionalProperties(additionalProperties);
        }

        Object delta = invokeNoArg(candidate, METHOD_DELTA);
        if (delta != null) {
            Optional<String> deltaReasoning = extractReasoningFromCandidate(delta);
            if (deltaReasoning.isPresent()) {
                return deltaReasoning;
            }
        }

        Object additionalProperties = invokeNoArg(candidate, METHOD_ADDITIONAL_PROPERTIES);
        if (additionalProperties instanceof Map<?, ?> properties) {
            return extractReasoningFromAdditionalProperties(properties);
        }
        return Optional.empty();
    }

    private Optional<String> extractReasoningFromAdditionalProperties(Map<?, ?> additionalProps) {
        if (additionalProps == null || additionalProps.isEmpty()) {
            return Optional.empty();
        }
        for (String key : ADDITIONAL_PROP_KEYS) {
            Object value = additionalProps.get(key);
            String s = extractString(value);
            if (s != null && !s.isBlank()) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    private String extractString(Object value) {
        if (value instanceof String s) {
            return s;
        }
        Object asString = invokeNoArg(value, METHOD_AS_STRING);
        if (asString instanceof Optional<?> optional) {
            Object content = optional.orElse(null);
            return content instanceof String str ? str : null;
        }
        return null;
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            log.trace("Method {} not available on {}", methodName, target.getClass().getName());
            return null;
        }
    }

    private void forward(String text, ClientStreamExecutionContext state, Consumer<String> thinkingConsumer) {
        if (text == null || text.isBlank()) {
            return;
        }
        state.thinkingText.append(text);
        if (thinkingConsumer != null) {
            thinkingConsumer.accept(text);
        }
    }

    /**
     * 流完成时将累积的思维链桥接到 ChatContext，供下游上下文读取。
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
