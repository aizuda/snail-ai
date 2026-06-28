package com.aizuda.snail.ai.common.constants;

import java.math.BigDecimal;

/**
 * 记忆模块与向量检索共用的默认配置（避免魔法数散落各层）。
 */
public final class MemoryIntegrationDefaults {

    private MemoryIntegrationDefaults() {
    }

    /** 未显式指定嵌入模型时的默认模型 ID（与库内默认模型记录一致） */
    public static final long DEFAULT_EMBEDDING_MODEL_ID = 1L;

    /**
     * 按 Agent 拉取「最近记忆」且未按类型过滤时的回溯天数（约 10 年，等价于「全部近期」）。
     */
    public static final int RECENT_MEMORIES_LOOKBACK_DAYS = 3650;

    /** 新增记忆时的默认相关性评分 */
    public static final BigDecimal DEFAULT_RELEVANCE_SCORE = BigDecimal.valueOf(0.8);

    /** 新增记忆时的默认置信度评分 */
    public static final BigDecimal DEFAULT_CONFIDENCE_SCORE = BigDecimal.valueOf(0.8);
}
