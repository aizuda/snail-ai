package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆压缩策略枚举，标识长期记忆的压缩方式。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum CompressionStrategyEnum {

    SLIDING_WINDOW(1, "滑动窗口"),
    IMPORTANCE_BASED(2, "基于重要性");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer strategy;
    /**
     * 枚举描述（中文）
     */
    private final String description;

    public static CompressionStrategyEnum fromStrategy(Integer strategy) {
        if (strategy == null) {
            return null;
        }
        for (CompressionStrategyEnum e : values()) {
            if (e.strategy.equals(strategy)) {
                return e;
            }
        }
        return null;
    }

    public static CompressionStrategyEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase()) {
            case "SLIDING_WINDOW" -> SLIDING_WINDOW;
            case "IMPORTANCE_BASED" -> IMPORTANCE_BASED;
            default -> null;
        };
    }
}
