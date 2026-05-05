package com.aizuda.snail.ai.admin.vo.knowledge;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeQueryVO extends BaseQueryVO {

    private String name;
}
