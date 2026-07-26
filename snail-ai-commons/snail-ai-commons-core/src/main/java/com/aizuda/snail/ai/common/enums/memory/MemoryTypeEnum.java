package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆类型枚举，标识一条记忆的语义分类。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum MemoryTypeEnum {

    FACT(1, "事实"),
    DECISION(2, "决策"),
    PREFERENCE(3, "偏好"),
    TASK_PROGRESS(4, "任务进度"),
    REFERENCE(5, "参考资料");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer type;
    /**
     * 枚举描述（中文）
     */
    private final String description;

    public static MemoryTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (MemoryTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    public static MemoryTypeEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase()) {
            case "FACT" -> FACT;
            case "DECISION" -> DECISION;
            case "PREFERENCE" -> PREFERENCE;
            case "TASK_PROGRESS" -> TASK_PROGRESS;
            case "REFERENCE" -> REFERENCE;
            default -> null;
        };
    }
}
