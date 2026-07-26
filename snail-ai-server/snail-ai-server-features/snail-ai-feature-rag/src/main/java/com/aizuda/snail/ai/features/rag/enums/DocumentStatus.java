package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 文章的处理状态
 * </p>
 *
 * @author opensnail
 * @since 2025-07-19
 */
@AllArgsConstructor
@Getter
public enum DocumentStatus {
    /** 初始（待处理） */
    INIT(0),
    /** 处理中 */
    PROCESSING(1),
    /** 处理成功 */
    SUCCESS(2),
    /** 处理失败 */
    FAIL(3);

    private final int status;

}
