package com.aizuda.snail.ai.agent.core.advisor;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code ThinkingCollectorAdvisor} 单元测试，验证从助手消息元数据中收集思考（reasoning）内容。
 *
 * @author opensnail
 * @since 2026-06-21
 */
class ThinkingCollectorAdvisorTest {

    @Test
    void shouldCollectReasoningContentFromAssistantMetadata() {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        StringBuilder received = new StringBuilder();
        Consumer<String> thinkingConsumer = received::append;

        ChatClientRequest request = new ChatClientRequest(new Prompt("test"), Map.of(
                ClientAdvisorKeys.STREAM_STATE, state,
                ClientAdvisorKeys.THINKING_CONSUMER, thinkingConsumer));
        ChatClientResponse response = buildOutputMetadataResponse("reasoning content");

        new ThinkingCollectorAdvisor()
                .adviseStream(request, new ResponseStreamAdvisorChain(List.of(response)))
                .blockLast();

        assertEquals("reasoning content", received.toString());
        assertEquals("reasoning content", state.thinkingText.toString());
    }

    @Test
    void shouldPreferReasoningTextOverSummary() {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        StringBuilder received = new StringBuilder();
        ChatClientRequest request = request(state, received);
        ReasoningPart reasoning = new ReasoningPart("reasoning delta", "summary delta", null, Map.of());

        new ThinkingCollectorAdvisor()
                .adviseStream(request, new ResponseStreamAdvisorChain(List.of(buildReasoningPartResponse(reasoning))))
                .blockLast();

        assertEquals("reasoning delta", received.toString());
        assertEquals("reasoning delta", state.thinkingText.toString());
    }

    @Test
    void shouldUseReasoningSummaryWhenTextIsMissing() {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        StringBuilder received = new StringBuilder();
        ChatClientRequest request = request(state, received);
        ReasoningPart reasoning = new ReasoningPart(null, "summary delta", null, Map.of());

        new ThinkingCollectorAdvisor()
                .adviseStream(request, new ResponseStreamAdvisorChain(List.of(buildReasoningPartResponse(reasoning))))
                .blockLast();

        assertEquals("summary delta", received.toString());
    }

    @Test
    void shouldAppendOnlyNewSuffixFromCumulativeGenerationMetadata() {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        StringBuilder received = new StringBuilder();
        ChatClientRequest request = request(state, received);

        new ThinkingCollectorAdvisor()
                .adviseStream(request, new ResponseStreamAdvisorChain(List.of(
                        buildGenerationMetadataResponse("first"),
                        buildGenerationMetadataResponse("first second"))))
                .blockLast();

        assertEquals("first second", received.toString());
        assertEquals("first second", state.thinkingText.toString());
    }

    private ChatClientRequest request(ClientStreamExecutionContext state, StringBuilder received) {
        return new ChatClientRequest(new Prompt("test"), Map.of(
                ClientAdvisorKeys.STREAM_STATE, state,
                ClientAdvisorKeys.THINKING_CONSUMER, (Consumer<String>) received::append));
    }

    private ChatClientResponse buildOutputMetadataResponse(String thinking) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .properties(Map.of("reasoningContent", thinking))
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        return new ChatClientResponse(chatResponse, Map.of());
    }

    private ChatClientResponse buildReasoningPartResponse(ReasoningPart reasoning) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .reasoning(reasoning)
                .build();
        return new ChatClientResponse(new ChatResponse(List.of(new Generation(assistantMessage))), Map.of());
    }

    private ChatClientResponse buildGenerationMetadataResponse(String thinking) {
        AssistantMessage assistantMessage = AssistantMessage.builder().content("").build();
        ChatGenerationMetadata metadata = ChatGenerationMetadata.builder()
                .metadata("reasoningContent", thinking)
                .build();
        return new ChatClientResponse(
                new ChatResponse(List.of(new Generation(assistantMessage, metadata))), Map.of());
    }

    private record ResponseStreamAdvisorChain(List<ChatClientResponse> responses) implements StreamAdvisorChain {

        @Override
        public Flux<ChatClientResponse> nextStream(ChatClientRequest request) {
            return Flux.fromIterable(responses);
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
}
