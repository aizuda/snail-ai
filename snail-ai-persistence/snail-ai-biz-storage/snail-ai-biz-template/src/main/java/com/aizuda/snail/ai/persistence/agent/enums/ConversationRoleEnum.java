package com.aizuda.snail.ai.persistence.agent.enums;

import lombok.Getter;

/**
 * 对话消息角色枚举
 */
@Getter
public enum ConversationRoleEnum {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    ConversationRoleEnum(String value) {
        this.value = value;
    }

    public boolean matches(String role) {
        return value.equals(role);
    }
}
