package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DocumentStatusEnum
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum DocumentStatusEnum {

    PENDING(0, "待处理"),
    PARSING(1, "解析中"),
    PROCESSING(2, "处理中"),
    SUCCESS(3, "处理完成"),
    FAILED(4, "处理失败");

    @EnumValue
    private final Integer status;
    private final String description;
}
