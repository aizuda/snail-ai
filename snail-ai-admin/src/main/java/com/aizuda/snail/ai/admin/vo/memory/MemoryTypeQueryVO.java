package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryTypeQueryVO extends BaseQueryVO {
    private Integer type;
    private Integer limit = 20;
}
