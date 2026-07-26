package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储实例类型枚举，标识具体的向量库 / 检索后端实现。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum StoreInstanceTypeEnum {

    PG_VECTOR(1, "PG向量库"),
    MILVUS(2, "Milvus"),
    ELASTICSEARCH(3, "Elasticsearch"),
    PG_FULLTEXT(4, "PG全文检索");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer type;
    /**
     * 枚举描述（中文）
     */
    private final String description;

    public static StoreInstanceTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (StoreInstanceTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
