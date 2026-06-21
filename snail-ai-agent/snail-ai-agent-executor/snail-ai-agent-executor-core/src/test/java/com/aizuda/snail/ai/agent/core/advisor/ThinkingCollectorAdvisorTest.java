package com.aizuda.snail.ai.agent.core.advisor;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThinkingCollectorAdvisorTest {

    @Test
    void shouldCollectReasoningContentFromAssistantMetadata() {
        ClientStreamExecutionContext state = new ClientStreamExecutionContext();
        StringBuilder received = new StringBuilder();
        Consumer<String> thinkingConsumer = received::append;

        ChatClientRequest request = new ChatClientRequest(new Prompt("test"), Map.of(
                ClientAdvisorKeys.STREAM_STATE, state,
                ClientAdvisorKeys.THINKING_CONSUMER, thinkingConsumer));
        ChatClientResponse response = buildResponse("reasoning content");

        new ThinkingCollectorAdvisor()
                .adviseStream(request, new SingleResponseStreamAdvisorChain(response))
                .blockLast();

        assertEquals("reasoning content", received.toString());
        assertEquals("reasoning content", state.thinkingText.toString());
    }

    private ChatClientResponse buildResponse(String thinking) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .properties(Map.of("reasoningContent", thinking))
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        return new ChatClientResponse(chatResponse, Map.of());
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
}
