package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 文档上传请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagDocumentUploadRequestVO {

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    /** 文档名称 */
    private String name;

    /**
     * UPLOAD | URL | TEXT
     */
    private String sourceType;

    /**
     * Required when sourceType = URL
     */
    private String url;

    /**
     * Required when sourceType = TEXT
     */
    private String content;

    /**
     * 单次覆盖 RAG 默认去重策略：0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT
     * 为空则使用 RAG 配置
     */
    private Integer dedupStrategy;

    /**
     * 单次覆盖 RAG 默认冲突动作：0=REJECT 1=SKIP 2=OVERWRITE
     * 为空则使用 RAG 配置
     */
    private Integer dedupAction;
}
