package com.aizuda.snail.ai.model.chat.openai.responses;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsesOutputTextNormalizerTest {

    @Test
    void shouldFillMissingAndNullAnnotationsWithoutReplacingExistingValues() {
        String missing = ResponsesOutputTextNormalizer.normalize(
                "{\"content\":[{\"type\":\"output_text\",\"text\":\"hi\"}]}");
        String explicitNull = ResponsesOutputTextNormalizer.normalize(
                "{\"content\":[{\"type\":\"output_text\",\"annotations\":null,\"text\":\"hi\"}]}");
        String existing = "{\"content\":[{\"type\":\"output_text\",\"annotations\":[{\"type\":\"url_citation\"}],\"text\":\"hi\"}]}";

        assertTrue(missing.contains("\"annotations\":[]"));
        assertTrue(explicitNull.contains("\"annotations\":[]"));
        assertEquals(existing, ResponsesOutputTextNormalizer.normalize(existing));
    }

    @Test
    void shouldNormalizeOnlySseDataLines() {
        String event = """
                event: response.completed
                data: {"response":{"output":[{"content":[{"type":"output_text","text":"done"}]}]}}
                """.strip();

        String normalized = ResponsesOutputTextNormalizer.normalizeSseEvent(event);

        assertTrue(normalized.startsWith("event: response.completed"));
        assertTrue(normalized.contains("\"annotations\":[]"));
    }

    @Test
    void shouldEmitNormalizedSseEventsBeforeTheStreamEnds() throws Exception {
        Buffer upstream = new Buffer();
        upstream.writeUtf8("data: {\"type\":\"response.output_text.delta\",\"delta\":\"A\"}\n\n");
        upstream.writeUtf8("data: {\"type\":\"response.completed\",\"response\":{\"output\":"
                + "[{\"content\":[{\"type\":\"output_text\",\"text\":\"A\"}]}]}}\n\n");
        BufferedSource source = Okio.buffer(
                new ResponsesOutputTextCompatibilityInterceptor.EventStreamSource(upstream));

        String body = source.readUtf8();

        assertTrue(body.contains("\"delta\":\"A\""));
        assertTrue(body.contains("\"annotations\":[]"));
        assertTrue(body.indexOf("\"delta\":\"A\"") < body.indexOf("\"annotations\":[]"));
    }
}
