package com.aizuda.snail.ai.model.enums;

/**
 * 模型作用域枚举
 */
public enum ModelScopeEnum {
    /**
     * 全局模型 - Admin配置
     */
    GLOBAL("GLOBAL", "全局"),

    /**
     * 个人模型 - 用户个人配置
     */
    PERSONAL("PERSONAL", "个人");

    private final String value;
    private final String name;

    ModelScopeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static ModelScopeEnum fromValue(String value) {
        for (ModelScopeEnum scope : ModelScopeEnum.values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        return null;
    }
}
