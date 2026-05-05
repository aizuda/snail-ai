package com.aizuda.snail.ai.agent.core.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 将模型输出的文本增量回调给 {@link ClientAdvisorKeys#CHUNK_CONSUMER}，并累积到 {@link ClientStreamExecutionContext}。
 */
public class StreamChunkForwarderAdvisor implements StreamAdvisor {

    @Override
    public String getName() {
        return "StreamChunkForwarderAdvisor";
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        @SuppressWarnings("unchecked")
        Consumer<String> chunkConsumer = (Consumer<String>) request.context().get(ClientAdvisorKeys.CHUNK_CONSUMER);
        Object st = request.context().get(ClientAdvisorKeys.STREAM_STATE);
        ClientStreamExecutionContext state = st instanceof ClientStreamExecutionContext c ? c : null;

        return chain.nextStream(request).doOnNext(response -> {
            String text = extractText(response);
            if (text != null && !text.isEmpty()) {
                if (state != null) {
                    state.fullText.append(text);
                }
                if (chunkConsumer != null) {
                    chunkConsumer.accept(text);
                }
            }
        });
    }

    private static String extractText(ChatClientResponse response) {
        return Optional.ofNullable(response)
                .map(ChatClientResponse::chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .orElse(null);
    }
}
