package com.aizuda.snail.ai.common.openapi.dto;

import com.aizuda.snail.ai.common.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI chat stream event.
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiChatStreamEvent {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_THINKING = "thinking";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    private String type;

    private String data;

    public static OpenApiChatStreamEvent of(String type, String data) {
        return OpenApiChatStreamEvent.builder()
                .type(type)
                .data(data)
                .build();
    }

    public static OpenApiChatStreamEvent text(String data) {
        return of(TYPE_TEXT, data);
    }

    public static OpenApiChatStreamEvent thinking(String data) {
        return of(TYPE_THINKING, data);
    }

    public static OpenApiChatStreamEvent done(String conversationId, String fullText) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversationId", conversationId);
        data.put("fullText", fullText);
        return of(TYPE_DONE, JsonUtil.toJsonString(data));
    }

    public static OpenApiChatStreamEvent error(String message) {
        return of(TYPE_ERROR, JsonUtil.toJsonString(Map.of(
                "message", message != null ? message : "Unknown error")));
    }
}
