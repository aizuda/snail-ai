package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentAnalyticsVO {

    private Integer activeUsers;

    private List<Integer> activeUsersTrend;

    private Integer conversationCount;

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

    private DateRange dateRange;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DateRange {
        private String start;
        private String end;
    }
}
