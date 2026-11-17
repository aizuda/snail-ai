package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChatStreamObserverTest {

    @Test
    void shouldMarkAgentErrorFrameAsFailed() {
        CapturingChatStreamWriter writer = new CapturingChatStreamWriter();
        ChatStreamObserver observer = createObserver(writer);
        ChatStreamResponse error = ChatStreamResponse.error("sid-1", "LLM_FAILED", "model unavailable");

        observer.onNext(GrpcSnailAiResult.newBuilder()
                .setData(JsonUtil.toJsonString(error))
                .build());

        assertEquals("[ERROR] model unavailable", writer.data);
        assertEquals("model unavailable", writer.error.getMessage());
        assertInstanceOf(SnailAiException.class, writer.error);
    }

    @Test
    void shouldPropagateGrpcTransportError() {
        CapturingChatStreamWriter writer = new CapturingChatStreamWriter();
        ChatStreamObserver observer = createObserver(writer);
        IllegalStateException failure = new IllegalStateException("connection lost");

        observer.onError(failure);

        assertEquals("[ERROR] Client disconnected: connection lost", writer.data);
        assertSame(failure, writer.error);
    }

    private ChatStreamObserver createObserver(ChatStreamWriter writer) {
        AgentChatContext context = new AgentChatContext(
                1L,
                "conversation-1",
                "hello",
                writer,
                null,
                null);
        return new ChatStreamObserver(context, null, 20);
    }

    private static class CapturingChatStreamWriter implements ChatStreamWriter {

        private String data;
        private Throwable error;

        @Override
        public void send(String data) {
            this.data = data;
        }

        @Override
        public void complete() {
        }

        @Override
        public void completeWithError(Throwable ex) {
            error = ex;
        }
    }
}
