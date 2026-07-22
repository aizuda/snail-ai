package com.aizuda.snail.ai.features.rag.enums;

import lombok.Getter;

/**
 * RAG chunk source type.
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Getter
public enum RagChunkSourceTypeEnum {

    TEXT("TEXT"),
    IMAGE("IMAGE");

    private final String value;

    RagChunkSourceTypeEnum(String value) {
        this.value = value;
    }

    public static boolean isImage(String value) {
        return IMAGE.value.equals(value);
    }
}
