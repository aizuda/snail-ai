package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompressionStrategyEnum {

    SLIDING_WINDOW(1, "滑动窗口"),
    IMPORTANCE_BASED(2, "基于重要性");

    @EnumValue
    private final Integer strategy;
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
