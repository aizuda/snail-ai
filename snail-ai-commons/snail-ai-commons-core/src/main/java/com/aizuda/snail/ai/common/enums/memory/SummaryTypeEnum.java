package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SummaryTypeEnum {

    INCREMENTAL(1, "增量摘要"),
    // TODO(memory-summary): FULL 为预留能力，当前压缩链路仅使用 INCREMENTAL
    FULL(2, "完整摘要");

    @EnumValue
    private final Integer type;
    private final String description;

    public static SummaryTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (SummaryTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    public static SummaryTypeEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase()) {
            case "INCREMENTAL" -> INCREMENTAL;
            case "FULL" -> FULL;
            default -> null;
        };
    }
}
