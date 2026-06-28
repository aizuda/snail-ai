package com.aizuda.snail.ai.features.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RAG 文档处理状态：待处理 / 解析中 / 处理中 / 成功 / 失败。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum RagDocumentStatus {

    PENDING(0),
    PARSING(1),
    PROCESSING(2),
    SUCCESS(3),
    FAILED(4);

    @EnumValue
    private final Integer status;
}
