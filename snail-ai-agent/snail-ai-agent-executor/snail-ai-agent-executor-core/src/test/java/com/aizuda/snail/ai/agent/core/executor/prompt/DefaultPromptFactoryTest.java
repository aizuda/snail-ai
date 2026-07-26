package com.aizuda.snail.ai.agent.core.executor.prompt;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultPromptFactoryTest {

    @Test
    void shouldAttachImagesToCurrentUserMessage() {
        DefaultPromptFactory factory = new DefaultPromptFactory();
        String base64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));

        Prompt prompt = factory.build(ChatDispatchRequest.builder()
                .userMessage("看图说明")
                .mediaAttachments(List.of(ChatDispatchRequest.MediaAttachment.builder()
                        .resourceId(12L)
                        .type("IMAGE")
                        .fileName("screen.png")
                        .mimeType("image/png")
                        .base64(base64)
                        .build()))
                .build());

        UserMessage message = assertInstanceOf(UserMessage.class, prompt.getInstructions().get(0));
        assertEquals("看图说明", message.getText());
        assertEquals(1, message.getMedia().size());
        assertEquals("image/png", message.getMedia().get(0).getMimeType().toString());
        assertArrayEquals("img".getBytes(StandardCharsets.UTF_8), (byte[]) message.getMedia().get(0).getData());
    }
}
