package com.aizuda.snail.ai.common.log;

import com.aizuda.snail.ai.common.log.strategy.Local;
import com.aizuda.snail.ai.common.log.strategy.Remote;

/**
 * 静态日志类 (复制自 snail-job，使用简化版 Local/Remote)
 */
public final class SnailAiLog {
    private SnailAiLog() {
    }

    public static final Local LOCAL = new Local();
    public static final Remote REMOTE = new Remote();
}
