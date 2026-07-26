package com.aizuda.snail.ai.feature.agent.enums;

/**
 * 智能体对话附件类型枚举。
 *
 * @author opensnail
 * @since 2026-07-07
 */
public enum AgentChatAttachmentTypeEnum {

    /**
     * 图片附件。
     */
    IMAGE("IMAGE");

    private final String value;

    AgentChatAttachmentTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String type) {
        return type != null && value.equalsIgnoreCase(type);
    }
}
