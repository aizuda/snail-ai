package com.aizuda.snail.ai.features.resource.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源业务类型枚举，标识资源所归属的业务场景。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Getter
@AllArgsConstructor
public enum ResourceBizTypeEnum {

    /**
     * 通用资源（未明确归类）
     */
    GENERAL("GENERAL"),
    /**
     * 用户头像
     */
    AVATAR("AVATAR"),
    /**
     * 普通附件
     */
    ATTACHMENT("ATTACHMENT"),
    /**
     * 对话图片附件
     */
    CHAT_IMAGE("CHAT_IMAGE"),
    /**
     * 知识库正式文档
     */
    DOCUMENT("DOCUMENT"),
    /**
     * 知识库文档解析提取图片
     */
    DOCUMENT_IMAGE("DOCUMENT_IMAGE"),
    /** 知识库上传预览阶段的临时资源；commit 时通过引用提升为 DOCUMENT，cancel/超时则清理 */
    DOCUMENT_PREVIEW("DOCUMENT_PREVIEW");

    /**
     * 类型标识，用于业务归类与持久化
     */
    private final String value;
}
