package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * RAG 切片新增请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagChunkCreateRequestVO {

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    private Long ragId;
    /** 所属文档 ID */
    private Long documentId;
    /** 切片内容 */
    private String content;
}
