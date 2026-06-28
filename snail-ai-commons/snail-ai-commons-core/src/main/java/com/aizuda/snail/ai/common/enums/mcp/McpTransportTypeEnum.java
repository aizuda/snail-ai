package com.aizuda.snail.ai.common.enums.mcp;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 传输类型
 */
@Getter
@AllArgsConstructor
public enum McpTransportTypeEnum {

    SSE(1, "sse"),
    STREAMABLE_HTTP(2, "streamable_http"),
    STDIO(3, "stdio");

    @EnumValue
    private final Integer value;
    private final String protocol;

    public static McpTransportTypeEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (McpTransportTypeEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    public static McpTransportTypeEnum fromProtocol(String protocol) {
        if (protocol == null) {
            return null;
        }
        for (McpTransportTypeEnum e : values()) {
            if (e.protocol.equalsIgnoreCase(protocol)) {
                return e;
            }
        }
        return null;
    }
}
