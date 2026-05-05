package com.aizuda.snail.ai.common.enums.mcp;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum McpServerStatusEnum {

    DISCONNECTED(0, "未连接"),
    CONNECTED(1, "已连接"),
    ERROR(2, "异常");

    @EnumValue
    private final Integer status;
    private final String description;

    public static McpServerStatusEnum fromStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (McpServerStatusEnum e : values()) {
            if (e.status.equals(status)) {
                return e;
            }
        }
        return null;
    }

    public static McpServerStatusEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase()) {
            case "disconnected" -> DISCONNECTED;
            case "connected" -> CONNECTED;
            case "error" -> ERROR;
            default -> null;
        };
    }
}
