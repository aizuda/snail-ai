package com.aizuda.snail.ai.model.chat.openai.responses;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * 兼容未返回 {@code annotations} 的 Responses 输出。OpenAI Java SDK 把该字段视为必填。
 *
 * @author opensnail
 * @since 2026-11-18
 */
final class ResponsesOutputTextNormalizer {

    static final String OUTPUT_TEXT_TYPE = "output_text";
    static final String TYPE_FIELD = "type";
    static final String ANNOTATIONS_FIELD = "annotations";
    static final String DATA_PREFIX = "data:";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ResponsesOutputTextNormalizer() {
    }

    static String normalize(String payload) {
        if (payload == null || payload.isBlank() || !payload.contains(OUTPUT_TEXT_TYPE)) {
            return payload;
        }
        try {
            JsonNode root = MAPPER.readTree(payload);
            if (!fillMissingAnnotations(root)) {
                return payload;
            }
            return MAPPER.writeValueAsString(root);
        } catch (Exception ignored) {
            return payload;
        }
    }

    static String normalizeSseEvent(String event) {
        if (event == null || event.isEmpty() || !event.contains(OUTPUT_TEXT_TYPE)) {
            return event;
        }
        String separator = event.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = event.split("\\r?\\n", -1);
        StringBuilder normalized = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                normalized.append(separator);
            }
            normalized.append(normalizeDataLine(lines[index]));
        }
        return normalized.toString();
    }

    private static String normalizeDataLine(String line) {
        if (!line.startsWith(DATA_PREFIX)) {
            return line;
        }
        String raw = line.substring(DATA_PREFIX.length());
        int contentStart = 0;
        if (!raw.isEmpty() && raw.charAt(0) == ' ') {
            contentStart = 1;
        }
        String json = raw.substring(contentStart);
        return DATA_PREFIX + raw.substring(0, contentStart) + normalize(json);
    }

    private static boolean fillMissingAnnotations(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        boolean changed = false;
        if (node instanceof ObjectNode object) {
            if (isOutputText(object) && isMissingAnnotations(object.get(ANNOTATIONS_FIELD))) {
                object.set(ANNOTATIONS_FIELD, MAPPER.createArrayNode());
                changed = true;
            }
            for (Map.Entry<String, JsonNode> field : object.properties()) {
                changed |= fillMissingAnnotations(field.getValue());
            }
            return changed;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                changed |= fillMissingAnnotations(child);
            }
        }
        return changed;
    }

    private static boolean isOutputText(ObjectNode object) {
        JsonNode type = object.get(TYPE_FIELD);
        return type != null && type.isString() && OUTPUT_TEXT_TYPE.equals(type.asString());
    }

    private static boolean isMissingAnnotations(JsonNode annotations) {
        return annotations == null || annotations.isNull() || annotations.isMissingNode();
    }
}
