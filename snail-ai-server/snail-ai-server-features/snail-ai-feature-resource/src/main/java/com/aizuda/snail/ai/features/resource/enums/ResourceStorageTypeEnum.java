package com.aizuda.snail.ai.features.resource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源存储类型枚举，标识资源所采用的存储后端。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum ResourceStorageTypeEnum {

    /**
     * 本地文件系统存储
     */
    LOCAL("LOCAL"),
    /**
     * MinIO 对象存储
     */
    MINIO("MINIO"),
    /**
     * 外部链接（仅记录访问地址，不托管文件）
     */
    EXTERNAL("EXTERNAL");

    /**
     * 类型标识，用于策略匹配与持久化
     */
    private final String value;

    public static ResourceStorageTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ResourceStorageTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
