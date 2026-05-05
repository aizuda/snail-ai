package com.aizuda.snail.ai.common.enums.agent;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 状态枚举
 */
@Getter
@AllArgsConstructor
public enum AgentStatusEnum {

    ACTIVE(1, "活跃"),
    INACTIVE(2, "非活跃"),
    DEPRECATED(3, "已废弃"),
    DISABLED(4, "已禁用");

    /** MyBatis-Plus 映射到数据库的值 */
    @EnumValue
    private final Integer status;
    private final String description;

    /**
     * 根据状态值获取枚举
     */
    public static AgentStatusEnum fromStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (AgentStatusEnum e : values()) {
            if (e.status.equals(status)) {
                return e;
            }
        }
        return null;
    }
}
