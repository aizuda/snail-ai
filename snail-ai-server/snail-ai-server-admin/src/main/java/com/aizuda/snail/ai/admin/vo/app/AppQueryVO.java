package com.aizuda.snail.ai.admin.vo.app;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryVO extends BaseQueryVO {
    private String keyword;
    private Integer status;
}
