package com.aizuda.snail.ai.admin.dto;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatStreamEventTest {

    @Test
    void shouldCreateCompletionEventWithUsage() {
        ChatStreamResponse completion = ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", null, 120, 40, 20, 100L);

        ChatStreamEvent event = ChatStreamEvent.completion(completion);

        assertEquals(ChatStreamResponse.TYPE_COMPLETION, event.getType());
        assertEquals(120, event.getUsage().getPromptTokens());
        assertEquals(40, event.getUsage().getCompletionTokens());
        assertEquals(20, event.getUsage().getCacheTokens());
        assertEquals(160, event.getUsage().getTotalTokens());
    }

    @Test
    void shouldOmitUsageForLegacyAllZeroCompletion() {
        ChatStreamResponse completion = ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", null, 0, 0, 0, 100L);

        ChatStreamEvent event = ChatStreamEvent.completion(completion);

        assertEquals(ChatStreamResponse.TYPE_COMPLETION, event.getType());
        assertNull(event.getUsage());
    }

    @Test
    void shouldNormalizeMissingAndNegativeTokenCounts() {
        ChatStreamResponse completion = ChatStreamResponse.completionWithUsage(
                "sid-1", "answer", null, null, 8, -2, 100L);

        ChatStreamEvent.ChatUsage usage = ChatStreamEvent.completion(completion).getUsage();

        assertEquals(0, usage.getPromptTokens());
        assertEquals(8, usage.getCompletionTokens());
        assertEquals(0, usage.getCacheTokens());
        assertEquals(8, usage.getTotalTokens());
    }
}
