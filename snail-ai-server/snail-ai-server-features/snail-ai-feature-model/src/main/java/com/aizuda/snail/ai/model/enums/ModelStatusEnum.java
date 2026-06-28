package com.aizuda.snail.ai.model.enums;

/**
 * 模型状态枚举
 */
public enum ModelStatusEnum {
    /**
     * 已启用
     */
    ENABLED(1, "已启用"),

    /**
     * 已禁用
     */
    DISABLED(0, "已禁用");

    private final int code;
    private final String description;

    ModelStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ModelStatusEnum fromCode(int code) {
        for (ModelStatusEnum status : ModelStatusEnum.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
