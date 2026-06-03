package com.aizuda.snail.ai.common.enums.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RAG 调用方式枚举
 */
@Getter
@AllArgsConstructor
public enum RagCallModeEnum {

    SMART(1, "智能调用"),
    FORCED(2, "强制调用");

    private final int mode;
    private final String desc;

    public static RagCallModeEnum of(Integer mode) {
        if (mode == null) {
            return SMART;
        }
        for (RagCallModeEnum e : values()) {
            if (e.mode == mode) {
                return e;
            }
        }
        return SMART;
    }
}
