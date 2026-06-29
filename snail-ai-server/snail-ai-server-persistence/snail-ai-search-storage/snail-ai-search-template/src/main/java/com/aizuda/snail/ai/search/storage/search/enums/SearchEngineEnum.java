package com.aizuda.snail.ai.search.storage.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全文检索引擎类型枚举，标识当前启用的搜索引擎实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum SearchEngineEnum {

    /**
     * Elasticsearch 全文检索引擎
     */
    ELASTICSEARCH("ELASTICSEARCH");

    /**
     * 类型标识，用于配置匹配与实例选择
     */
    private final String type;

    public static SearchEngineEnum fromType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        for (SearchEngineEnum value : values()) {
            if (value.type.equalsIgnoreCase(type.trim())) {
                return value;
            }
        }
        return null;
    }
}
