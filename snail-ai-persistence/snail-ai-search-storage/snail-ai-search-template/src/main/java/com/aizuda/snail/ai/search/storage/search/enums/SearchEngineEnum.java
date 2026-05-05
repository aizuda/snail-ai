package com.aizuda.snail.ai.search.storage.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchEngineEnum {

    ELASTICSEARCH("ELASTICSEARCH");

    private final String type;

    public static SearchEngineEnum fromType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        for (SearchEngineEnum value : values()) {
            if (value.type.equalsIgnoreCase(type.trim())) {
                return value;
            }
        }
        return null;
    }
}
