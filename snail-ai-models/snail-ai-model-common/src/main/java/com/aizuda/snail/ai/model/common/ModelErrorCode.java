package com.aizuda.snail.ai.model.common;

/**
 * 模型适配错误码枚举，标识模型适配/构建过程中的失败类型。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public enum ModelErrorCode {
    /** 模型配置非法 */
    INVALID_MODEL_CONFIG,
    /** 未找到匹配的适配器 */
    ADAPTER_NOT_FOUND,
    /** 模型构建失败 */
    MODEL_BUILD_FAILED
}
