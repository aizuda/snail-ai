package com.aizuda.snail.ai.admin.vo.mcp;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 服务端分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerQueryVO extends BaseQueryVO {

    /** 关键字（名称/描述模糊查询） */
    private String keyword;

    /** 传输类型 */
    private Integer transportType;
}
