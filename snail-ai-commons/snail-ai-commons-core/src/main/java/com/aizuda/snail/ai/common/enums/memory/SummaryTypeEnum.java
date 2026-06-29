package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆摘要类型枚举，标识会话摘要的生成方式。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum SummaryTypeEnum {

    INCREMENTAL(1, "增量摘要"),
    // TODO(memory-summary): FULL 为预留能力，当前压缩链路仅使用 INCREMENTAL
    FULL(2, "完整摘要");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer type;
    /**
     * 枚举描述（中文）
     */
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
