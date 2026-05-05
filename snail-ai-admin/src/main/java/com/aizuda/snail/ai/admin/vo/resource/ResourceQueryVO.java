package com.aizuda.snail.ai.admin.vo.resource;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceQueryVO extends BaseQueryVO {
    private String bizType;
    private String originalName;
    private Long bizId;
}
