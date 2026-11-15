package com.aizuda.snail.ai.common.enums.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Responses 首期支持的 Hosted Tool 类型。
 *
 * @author opensnail
 * @since 2026-11-14
 */
@Getter
@AllArgsConstructor
public enum HostedToolTypeEnum {

    WEB_SEARCH("web_search"),
    FILE_SEARCH("file_search"),
    CODE_INTERPRETER("code_interpreter"),
    IMAGE_GENERATION("image_generation"),
    RAW("raw");

    private final String value;

    public static HostedToolTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (HostedToolTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}
