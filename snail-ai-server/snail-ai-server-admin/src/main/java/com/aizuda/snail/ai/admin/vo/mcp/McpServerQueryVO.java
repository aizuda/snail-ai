package com.aizuda.snail.ai.admin.vo.mcp;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * McpServerQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerQueryVO extends BaseQueryVO {

    private String keyword;

    private Integer transportType;
}
