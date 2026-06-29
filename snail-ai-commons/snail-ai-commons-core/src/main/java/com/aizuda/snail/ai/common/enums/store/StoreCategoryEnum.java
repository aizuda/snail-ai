package com.aizuda.snail.ai.common.enums.store;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储类别枚举，区分向量库与搜索引擎两大类存储。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum StoreCategoryEnum {

    VECTOR_STORE(1, "VECTOR_STORE"),
    SEARCH_ENGINE(2, "SEARCH_ENGINE");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer category;
    /**
     * 类别编码（字符串标识，用于兼容旧值匹配）
     */
    private final String code;

    public static StoreCategoryEnum fromValue(Integer value) {
        if (value == null) {
            return VECTOR_STORE;
        }
        for (StoreCategoryEnum e : values()) {
            if (String.valueOf(e.category).equals(String.valueOf(value))) {
                return e;
            }
        }
        return VECTOR_STORE;
    }

    public static StoreCategoryEnum fromLegacy(String value) {
        if (value == null || value.isBlank()) {
            return VECTOR_STORE;
        }
        for (StoreCategoryEnum e : values()) {
            if (e.code.equalsIgnoreCase(value.trim())) {
                return e;
            }
        }
        return VECTOR_STORE;
    }
}
