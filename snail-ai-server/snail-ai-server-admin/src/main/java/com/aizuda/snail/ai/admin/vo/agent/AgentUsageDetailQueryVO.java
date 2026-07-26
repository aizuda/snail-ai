package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体使用明细查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentUsageDetailQueryVO extends BaseQueryVO {

    /** 统计开始日期 */
    private String start;

    /** 统计结束日期 */
    private String end;
}
