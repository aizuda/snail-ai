package com.aizuda.snail.ai.features.resource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceBizTypeEnum {

    GENERAL("GENERAL"),
    AVATAR("AVATAR"),
    ATTACHMENT("ATTACHMENT"),
    DOCUMENT("DOCUMENT"),
    /** 知识库上传预览阶段的临时资源；commit 时通过引用提升为 DOCUMENT，cancel/超时则清理 */
    DOCUMENT_PREVIEW("DOCUMENT_PREVIEW");

    private final String value;
}
