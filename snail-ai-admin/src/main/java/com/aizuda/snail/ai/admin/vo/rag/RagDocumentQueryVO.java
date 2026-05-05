package com.aizuda.snail.ai.admin.vo.rag;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RagDocumentQueryVO extends BaseQueryVO {

    @JsonAlias("knowledgeId")
    private Long ragId;

    private String name;

    private Integer status;
}
