package org.springframework.ai.openai;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiChatModelChunkMergerTest {

    @Test
    void shouldPreserveDeltaReasoningAdditionalProperties() throws Exception {
        ChatCompletionChunk chunk = ChatCompletionChunk.builder()
                .id("chatcmpl-test")
                .created(1L)
                .model("reasoning-model")
                .object_(JsonValue.from("chat.completion.chunk"))
                .addChoice(ChatCompletionChunk.Choice.builder()
                        .index(0L)
                        .finishReason(ChatCompletionChunk.Choice.FinishReason.STOP)
                        .delta(ChatCompletionChunk.Choice.Delta.builder()
                                .content("")
                                .putAdditionalProperty("reasoning_content", JsonValue.from("reasoning content"))
                                .build())
                        .build())
                .build();

        ChatCompletion completion = invokeChunkToChatCompletion(chunk);
        JsonValue reasoning = completion.choices()
                .getFirst()
                .message()
                ._additionalProperties()
                .get("reasoning_content");

        assertEquals("reasoning content", reasoning.asString().orElse(""));
    }

    private ChatCompletion invokeChunkToChatCompletion(ChatCompletionChunk chunk) throws Exception {
        Class<?> chunkMergerClass = Class.forName("org.springframework.ai.openai.OpenAiChatModel$ChunkMerger");
        Method method = chunkMergerClass.getDeclaredMethod("chunkToChatCompletion", ChatCompletionChunk.class);
        method.setAccessible(true);
        return (ChatCompletion) method.invoke(null, chunk);
    }
}
