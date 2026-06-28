package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆状态枚举
 */
@Getter
@AllArgsConstructor
public enum MemoryStatusEnum {

    ACTIVE(1, "激活"),
    ARCHIVED(2, "已归档"),
    SUPPRESSED(3, "已压制");

    /** MyBatis-Plus 映射到数据库的值 */
    @EnumValue
    private final Integer status;
    private final String description;

    public static MemoryStatusEnum fromStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (MemoryStatusEnum e : values()) {
            if (e.status.equals(status)) {
                return e;
            }
        }
        return null;
    }

    public static MemoryStatusEnum fromLegacy(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE" -> ACTIVE;
            case "ARCHIVED" -> ARCHIVED;
            case "SUPPRESSED" -> SUPPRESSED;
            default -> null;
        };
    }
}
