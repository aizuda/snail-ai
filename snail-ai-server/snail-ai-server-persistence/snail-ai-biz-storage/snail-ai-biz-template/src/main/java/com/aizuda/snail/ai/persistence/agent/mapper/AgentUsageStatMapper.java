package com.aizuda.snail.ai.persistence.agent.mapper;

import com.aizuda.snail.ai.persistence.agent.po.AgentUsageStatPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 智能体用量统计数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface AgentUsageStatMapper extends BaseMapper<AgentUsageStatPO> {

    /**
     * 汇总时间范围内的总量指标（会话数、消息数、活跃用户数）。
     * <p>
     * 库侧聚合为单行返回，避免把区间内全量明细行拉进内存后再累加。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 单行：totalConversations / totalMessages / activeUsers
     */
    @Select("""
            SELECT COALESCE(SUM(conversation_count), 0) AS totalConversations,
                   COALESCE(SUM(message_count), 0) AS totalMessages,
                   COUNT(DISTINCT user_id) AS activeUsers
            FROM sai_agent_usage_stat
            WHERE stat_date BETWEEN #{start} AND #{end}
            """)
    Map<String, Object> sumOverview(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 按日聚合趋势（会话数、消息数、活跃用户数、活跃智能体数）。
     * <p>
     * 库侧 {@code GROUP BY stat_date}，返回行数至多为区间天数，避免全量明细进内存。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 每个日期一行：statDate / conversationCount / messageCount / activeUsers / activeAgents
     */
    @Select("""
            SELECT stat_date AS statDate,
                   COALESCE(SUM(conversation_count), 0) AS conversationCount,
                   COALESCE(SUM(message_count), 0) AS messageCount,
                   COUNT(DISTINCT user_id) AS activeUsers,
                   COUNT(DISTINCT CASE WHEN conversation_count > 0 THEN agent_id END) AS activeAgents
            FROM sai_agent_usage_stat
            WHERE stat_date BETWEEN #{start} AND #{end}
            GROUP BY stat_date
            """)
    List<Map<String, Object>> aggregateDailyTrends(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 按智能体聚合会话数与消息数（用于智能体排行与资源分配的会话统计）。
     * <p>
     * 库侧 {@code GROUP BY agent_id}，返回行数至多为区间内活跃智能体数。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 每个智能体一行：agentId / conversationCount / messageCount
     */
    @Select("""
            SELECT agent_id AS agentId,
                   COALESCE(SUM(conversation_count), 0) AS conversationCount,
                   COALESCE(SUM(message_count), 0) AS messageCount
            FROM sai_agent_usage_stat
            WHERE stat_date BETWEEN #{start} AND #{end}
              AND agent_id IS NOT NULL
            GROUP BY agent_id
            """)
    List<Map<String, Object>> aggregateAgentTotals(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
