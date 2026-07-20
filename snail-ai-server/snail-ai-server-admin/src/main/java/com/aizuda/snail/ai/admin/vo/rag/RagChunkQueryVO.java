package com.aizuda.snail.ai.admin.vo.rag;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 切片分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RagChunkQueryVO extends BaseQueryVO {

    /** 所属文档 ID */
    private Long documentId;

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    private Long ragId;

    /** 搜索关键词（按 ID 或内容模糊匹配） */
    private String keyword;
}
