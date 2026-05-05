package com.aizuda.snail.ai.common.enums.store;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StoreCategoryEnum {

    VECTOR_STORE(1, "VECTOR_STORE"),
    SEARCH_ENGINE(2, "SEARCH_ENGINE");

    @EnumValue
    private final Integer category;
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
