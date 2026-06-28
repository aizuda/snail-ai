package com.aizuda.snail.ai.admin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Structured event for RAG QA stream.
 */
@Data
@Builder
public class RagQaStreamEvent {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    private String type;

    private String data;

    private String message;

    public static RagQaStreamEvent text(String data) {
        return RagQaStreamEvent.builder()
                .type(TYPE_TEXT)
                .data(data)
                .build();
    }

    public static RagQaStreamEvent done() {
        return RagQaStreamEvent.builder()
                .type(TYPE_DONE)
                .build();
    }

    public static RagQaStreamEvent error(String message) {
        return RagQaStreamEvent.builder()
                .type(TYPE_ERROR)
                .message(message)
                .build();
    }
}
