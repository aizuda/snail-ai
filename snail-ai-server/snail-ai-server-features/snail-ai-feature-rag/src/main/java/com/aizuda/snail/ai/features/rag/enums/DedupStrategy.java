package com.aizuda.snail.ai.features.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 知识库文档上传去重策略
 *
 * @author opensnail
 */
@Getter
@AllArgsConstructor
public enum DedupStrategy {

    /** 不去重 */
    NONE(0),

    /** 同库同名文件视为重复 */
    BY_NAME(1),

    /** 同库同 SHA-256 内容视为重复 */
    BY_CONTENT(2),

    /** 同名或同内容任一命中即视为重复 */
    BY_NAME_OR_CONTENT(3);

    @EnumValue
    private final Integer code;

    public boolean matchesByName() {
        return this == BY_NAME || this == BY_NAME_OR_CONTENT;
    }

    public boolean matchesByContent() {
        return this == BY_CONTENT || this == BY_NAME_OR_CONTENT;
    }

    public static DedupStrategy fromCode(Integer code) {
        if (code == null) {
            return BY_CONTENT;
        }
        for (DedupStrategy e : values()) {
            if (Objects.equals(e.code, code)) {
                return e;
            }
        }
        return BY_CONTENT;
    }
}
