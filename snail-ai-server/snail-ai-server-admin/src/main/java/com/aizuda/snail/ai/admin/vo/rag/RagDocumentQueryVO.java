package com.aizuda.snail.ai.admin.vo.rag;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 文档分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RagDocumentQueryVO extends BaseQueryVO {

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    private Long ragId;

    /** 文档名称（模糊查询） */
    private String name;

    /** 处理状态 */
    private Integer status;
}
