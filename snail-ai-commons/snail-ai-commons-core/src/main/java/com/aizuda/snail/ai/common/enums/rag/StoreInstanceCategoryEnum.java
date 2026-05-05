package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StoreInstanceCategoryEnum {

    VECTOR_STORE(1, "向量库"),
    SEARCH_ENGINE(2, "搜索引擎");

    @EnumValue
    private final Integer category;
    private final String description;

    public static StoreInstanceCategoryEnum fromCategory(Integer category) {
        if (category == null) {
            return null;
        }
        for (StoreInstanceCategoryEnum e : values()) {
            if (e.category.equals(category)) {
                return e;
            }
        }
        return null;
    }
}
