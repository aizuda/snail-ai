package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体数据分析查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAnalyticsQueryVO extends BaseQueryVO {

    /** 时间范围（如 7d/30d） */
    private String range = "7d";

    /** 自定义开始日期 */
    private String start;

    /** 自定义结束日期 */
    private String end;
}
