package com.aizuda.snail.ai.admin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Structured event for agent creation stream.
 */
@Data
@Builder
public class AgentCreateStreamEvent {

    public static final String TYPE_START = "start";
    public static final String TYPE_FIELD_DONE = "field_done";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_GREETING = "greeting";
    public static final String FIELD_PRESET_QUESTIONS = "presetQuestions";
    public static final String FIELD_INSTRUCTION = "instruction";

    private String type;

    private String field;

    private Object data;

    private Long agentId;

    private String message;

    public static AgentCreateStreamEvent start() {
        return AgentCreateStreamEvent.builder()
                .type(TYPE_START)
                .build();
    }

    public static AgentCreateStreamEvent fieldDone(String field, Object data) {
        return AgentCreateStreamEvent.builder()
                .type(TYPE_FIELD_DONE)
                .field(field)
                .data(data)
                .build();
    }

    public static AgentCreateStreamEvent done(Long agentId) {
        return AgentCreateStreamEvent.builder()
                .type(TYPE_DONE)
                .agentId(agentId)
                .build();
    }

    public static AgentCreateStreamEvent error(String message) {
        return AgentCreateStreamEvent.builder()
                .type(TYPE_ERROR)
                .message(message)
                .build();
    }
}
