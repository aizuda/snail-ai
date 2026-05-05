package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StoreInstanceTypeEnum {

    PG_VECTOR(1, "PG向量库"),
    MILVUS(2, "Milvus"),
    ELASTICSEARCH(3, "Elasticsearch"),
    PG_FULLTEXT(4, "PG全文检索");

    @EnumValue
    private final Integer type;
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
