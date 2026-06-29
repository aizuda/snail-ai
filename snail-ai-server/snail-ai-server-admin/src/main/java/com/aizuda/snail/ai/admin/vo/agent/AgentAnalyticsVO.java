package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体数据分析统计视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentAnalyticsVO {

    /** 活跃用户数 */
    private Integer activeUsers;

    /** 活跃用户数趋势（按日） */
    private List<Integer> activeUsersTrend;

    /** 会话数量 */
    private Integer conversationCount;

    /** 会话数量趋势（按日） */
    private List<Integer> conversationCountTrend;

    /** 总消息数 */
    private Integer totalMessages;

    /** 日期标签，与趋势数据一一对应 */
    private List<String> dateLabels;

    /** 消息趋势（按日） */
    private List<Integer> messageTrend;

    /** 总工具调用次数 */
    private Integer totalToolCalls;

    /** 平均响应时间(ms) */
    private Double avgResponseTime;

    /** 统计日期范围 */
    private DateRange dateRange;

    /**
     * 统计日期范围
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DateRange {
        /** 开始日期 */
        private String start;
        /** 结束日期 */
        private String end;
    }
}
