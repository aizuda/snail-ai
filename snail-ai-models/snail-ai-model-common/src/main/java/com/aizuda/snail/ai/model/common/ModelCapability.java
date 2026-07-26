package com.aizuda.snail.ai.model.common;

/**
 * 模型能力类型枚举，标识适配器支持的模型种类。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public enum ModelCapability {
    /** 对话 */
    CHAT,
    /** 向量化（嵌入） */
    EMBEDDING,
    /** 重排 */
    RERANKER
}
