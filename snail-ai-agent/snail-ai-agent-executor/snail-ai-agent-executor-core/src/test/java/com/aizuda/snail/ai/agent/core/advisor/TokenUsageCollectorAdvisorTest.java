package com.aizuda.snail.ai.agent.core.advisor;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link TokenUsageCollectorAdvisor} 缓存 Token 提取测试。
 *
 * @author opensnail
 * @since 2026-11-14
 */
class TokenUsageCollectorAdvisorTest {

    @Test
    void shouldReadCacheTokensFromGenericUsageGetter() {
        ClientChatExecutorResult result = collect(new DefaultUsage(20, 5, 25, null, 7L, 3L));

        assertEquals(20, result.promptTokens());
        assertEquals(5, result.completionTokens());
        assertEquals(7, result.cacheTokens());
    }

    @Test
    void shouldFallbackToNativeUsageForCompatibleModels() {
        Usage usage = new DefaultUsage(20, 5, 25, new NativeUsage(9L), null, null);
        ClientChatExecutorResult result = collect(usage);

        assertEquals(9, result.cacheTokens());
    }

    @Test
    void shouldNormalizeNegativeNativeIntegerCacheUsage() {
        Usage usage = new DefaultUsage(20, 5, 25, new NativeIntegerUsage(-1), null, null);
        ClientChatExecutorResult result = collect(usage);

        assertEquals(0, result.cacheTokens());
    }

    private ClientChatExecutorResult collect(Usage usage) {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        ChatClientRequest request = new ChatClientRequest(
                new Prompt("test"), Map.of(ClientAdvisorKeys.STREAM_STATE, state));
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        ChatClientResponse response = new ChatClientResponse(new ChatResponse(List.of(), metadata), Map.of());

        new TokenUsageCollectorAdvisor()
                .adviseStream(request, new SingleResponseStreamAdvisorChain(response))
                .blockLast();

        var completion = state.toCompletionResult();
        return new ClientChatExecutorResult(
                completion.promptTokens(), completion.completionTokens(), completion.cacheTokens());
    }

    public static final class NativeUsage {

        private final PromptTokensDetails details;

        private NativeUsage(long cachedTokens) {
            this.details = new PromptTokensDetails(cachedTokens);
        }

        public Optional<PromptTokensDetails> promptTokensDetails() {
            return Optional.of(details);
        }
    }

    public static final class PromptTokensDetails {

        private final long cachedTokens;

        private PromptTokensDetails(long cachedTokens) {
            this.cachedTokens = cachedTokens;
        }

        public Optional<Long> cachedTokens() {
            return Optional.of(cachedTokens);
        }
    }

    public record NativeIntegerUsage(IntegerPromptTokensDetails details) {

        public NativeIntegerUsage(int cachedTokens) {
            this(new IntegerPromptTokensDetails(cachedTokens));
        }

        public Optional<IntegerPromptTokensDetails> promptTokensDetails() {
            return Optional.of(details);
        }
    }

    public record IntegerPromptTokensDetails(int value) {

        public Optional<Integer> cachedTokens() {
            return Optional.of(value);
        }
    }

    private record SingleResponseStreamAdvisorChain(ChatClientResponse response) implements StreamAdvisorChain {

        @Override
        public Flux<ChatClientResponse> nextStream(ChatClientRequest request) {
            return Flux.just(response);
        }

        @Override
        public List<StreamAdvisor> getStreamAdvisors() {
            return List.of();
        }

        @Override
        public StreamAdvisorChain copy(StreamAdvisor advisor) {
            return this;
        }
    }

    private record ClientChatExecutorResult(int promptTokens, int completionTokens, int cacheTokens) {
    }
}
