package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MemoriesQueryVO extends BaseQueryVO {

    private Long agentId;
    private Long userId;
    private String conversationId;
    private Integer status;

}
