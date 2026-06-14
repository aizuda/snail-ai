package com.aizuda.snail.ai.admin.vo.mcp;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerQueryVO extends BaseQueryVO {

    private String keyword;

    private Integer status;

    private Integer transportType;
}
