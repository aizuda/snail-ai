package com.aizuda.snail.ai.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举（启用/禁用）
 *
 * @author opensnail
 * @date 2025-04-11
 */
@AllArgsConstructor
@Getter
public enum CommonStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final Integer value;
    private final String desc;
}
