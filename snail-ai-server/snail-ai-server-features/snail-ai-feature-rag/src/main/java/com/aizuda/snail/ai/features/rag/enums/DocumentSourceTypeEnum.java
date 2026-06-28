package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档导入来源类型
 */
@Getter
@AllArgsConstructor
public enum DocumentSourceTypeEnum {

    UPLOAD("UPLOAD"),
    URL("URL");

    private final String value;

    public static DocumentSourceTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentSourceTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
