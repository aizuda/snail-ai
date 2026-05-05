package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型
 */
@Getter
@AllArgsConstructor
public enum FileStorageTypeEnum {

    LOCAL("LOCAL");

    private final String value;

    public static FileStorageTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FileStorageTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
