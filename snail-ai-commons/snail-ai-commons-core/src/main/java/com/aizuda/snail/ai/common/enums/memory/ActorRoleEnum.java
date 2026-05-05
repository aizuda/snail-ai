package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActorRoleEnum {

    USER(1, "用户"),
    AGENT(2, "智能体"),
    SYSTEM(3, "系统");

    @EnumValue
    private final Integer role;
    private final String description;

    public static ActorRoleEnum fromRole(Integer role) {
        if (role == null) {
            return null;
        }
        for (ActorRoleEnum e : values()) {
            if (e.role.equals(role)) {
                return e;
            }
        }
        return null;
    }

    public static ActorRoleEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase()) {
            case "user" -> USER;
            case "assistant", "agent" -> AGENT;
            case "system" -> SYSTEM;
            default -> null;
        };
    }
}
