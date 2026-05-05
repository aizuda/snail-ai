package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChunkModeEnum {

    /** 按长度切片（纯递归切分，不做一级分隔） */
    DEFAULT("default"),
    /** 按分隔符切片（先按分隔符一级切分，再递归） */
    DELIMITER("delimiter"),
    /** 正则表达式切片（先按正则一级切分，再递归） */
    REGEX("regex"),
    /** 智能切片（LLM 语义分段，再递归） */
    SMART("smart");

    private final String mode;

    /**
     * 根据字符串值解析枚举，不区分大小写；无法匹配时返回 DEFAULT。
     */
    public static ChunkModeEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        for (ChunkModeEnum e : values()) {
            if (e.mode.equalsIgnoreCase(value.trim())) {
                return e;
            }
        }
        return DEFAULT;
    }
}
