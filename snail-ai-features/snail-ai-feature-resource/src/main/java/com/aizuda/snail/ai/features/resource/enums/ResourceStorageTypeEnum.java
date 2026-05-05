package com.aizuda.snail.ai.features.resource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceStorageTypeEnum {

    LOCAL("LOCAL"),
    MINIO("MINIO");

    private final String value;

    public static ResourceStorageTypeEnum fromValue(String value) {
        if (value == null) return null;
        for (ResourceStorageTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) return e;
        }
        return null;
    }
}
