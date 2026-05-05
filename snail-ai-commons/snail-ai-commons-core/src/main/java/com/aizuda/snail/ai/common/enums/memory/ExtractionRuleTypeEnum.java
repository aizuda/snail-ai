package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆提取规则指令类型
 */
@Getter
@AllArgsConstructor
public enum ExtractionRuleTypeEnum {

    /** 默认内置规则（推荐，适合大多数场景） */
    DEFAULT("DEFAULT", "默认规则指令"),

    /** 自定义规则指令（用户自行填写关注/忽略规则） */
    CUSTOM("CUSTOM", "自定义规则指令");

    @EnumValue
    private final String code;
    private final String description;

    public static ExtractionRuleTypeEnum fromCode(String code) {
        if (code == null) return DEFAULT;
        for (ExtractionRuleTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return DEFAULT;
    }
}
