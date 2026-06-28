package com.aizuda.snail.ai.common.enums.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 对话记录等状态
 *
 * @author opensnail
 * @date 2025-07-09
 */
@AllArgsConstructor
@Getter
public enum StatusEnum {

    RUNNING(1),
    SUCCESS(2),
    FAIL(3);

    private final Integer value;
}
