package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档导入来源类型
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum DocumentSourceTypeEnum {

    /** 本地上传文件 */
    UPLOAD("UPLOAD"),
    /** 远程 URL 导入 */
    URL("URL");

    private final String value;

    public static DocumentSourceTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentSourceTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
