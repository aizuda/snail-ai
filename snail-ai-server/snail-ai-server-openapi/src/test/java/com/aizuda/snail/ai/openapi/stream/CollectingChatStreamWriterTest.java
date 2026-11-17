package com.aizuda.snail.ai.openapi.stream;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectingChatStreamWriterTest {

    @Test
    void shouldCollectTextAndCompletionFrame() throws Exception {
        CollectingChatStreamWriter writer = new CollectingChatStreamWriter();
        ChatStreamResponse completion = ChatStreamResponse.completionWithUsage(
                "sid-1", "hello world", "", 7, 3, 2, 100L);

        writer.send(JsonUtil.toJsonString(ChatStreamResponse.text("hello ")));
        writer.send(JsonUtil.toJsonString(ChatStreamResponse.text("world")));
        writer.onCompletion(completion);
        writer.complete();

        CollectingChatStreamWriter.CollectedChatResult result = writer.awaitAndGetResult(100L);

        assertEquals("hello world", result.fullText());
        assertSame(completion, result.completion());
    }

    @Test
    void shouldTimeOutWhenStreamDoesNotComplete() {
        CollectingChatStreamWriter writer = new CollectingChatStreamWriter();

        assertThrows(TimeoutException.class, () -> writer.awaitAndGetResult(1L));
    }

    @Test
    void shouldPropagateStreamError() {
        CollectingChatStreamWriter writer = new CollectingChatStreamWriter();
        IllegalStateException failure = new IllegalStateException("model unavailable");
        writer.completeWithError(failure);

        SnailAiException exception = assertThrows(
                SnailAiException.class,
                () -> writer.awaitAndGetResult(100L));

        assertSame(failure, exception.getCause());
    }
}
