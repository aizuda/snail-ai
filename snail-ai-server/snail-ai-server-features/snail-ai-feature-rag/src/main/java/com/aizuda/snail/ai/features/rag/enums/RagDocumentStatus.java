package com.aizuda.snail.ai.features.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RagDocumentStatus {

    PENDING(0),
    PARSING(1),
    PROCESSING(2),
    SUCCESS(3),
    FAILED(4);

    @EnumValue
    private final Integer status;
}
