package com.aizuda.snail.ai.common.enums.memory;

/**
 * 记忆压缩策略
 */
public enum CompressionStrategy {

    /** 滑动窗口：保留最近 N 条，将更早的一批压缩为摘要并归档 */
    SLIDING_WINDOW,

    /** 重要性过滤（预留） */
    IMPORTANCE_BASED;

    public static CompressionStrategy fromConfig(Integer raw) {
        if (raw == null) {
            return SLIDING_WINDOW;
        }
        return raw == CompressionStrategyEnum.IMPORTANCE_BASED.getStrategy()
                ? IMPORTANCE_BASED
                : SLIDING_WINDOW;
    }
}
