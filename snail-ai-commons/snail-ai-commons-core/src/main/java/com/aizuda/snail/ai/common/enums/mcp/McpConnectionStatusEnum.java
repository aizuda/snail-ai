package com.aizuda.snail.ai.common.enums.mcp;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 连接状态
 */
@Getter
@AllArgsConstructor
public enum McpConnectionStatusEnum {

    DISCONNECTED(0, "未连接"),
    CONNECTED(1, "已连接"),
    ERROR(2, "异常");

    @EnumValue
    private final Integer value;
    private final String description;
}
