package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储实例类别枚举，区分存储实例属于向量库还是搜索引擎。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum StoreInstanceCategoryEnum {

    VECTOR_STORE(1, "向量库"),
    SEARCH_ENGINE(2, "搜索引擎");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer category;
    /**
     * 枚举描述（中文）
     */
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
