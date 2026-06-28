package com.aizuda.snail.ai.common.enums.mcp;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum McpAuthTypeEnum {

    NONE(0, "无需认证"),
    API_KEY(1, "API Key"),
    OAUTH(2, "OAuth"),
    BASIC(3, "Basic Auth");

    @EnumValue
    private final Integer type;
    private final String description;

    public static McpAuthTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (McpAuthTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    public static McpAuthTypeEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase()) {
            case "none" -> NONE;
            case "apikey", "api_key" -> API_KEY;
            case "oauth" -> OAUTH;
            case "basic" -> BASIC;
            default -> null;
        };
    }
}
