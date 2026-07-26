package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum FileStorageTypeEnum {

    /** 本地磁盘存储 */
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
