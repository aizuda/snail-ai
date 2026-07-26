package com.aizuda.snail.ai.persistence.agent.mapper;

import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelUsageStatPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 智能体对话记录数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface AgentConversationRecordMapper extends BaseMapper<AgentConversationRecordPO> {

    /**
     * 按模型维度读时聚合对话用量统计。
     * <p>
     * 数据源为对话消息记录（assistant 行携带 token），通过 agent.chat_model_id 归属到模型。
     * 仅统计携带 token 的消息（即真实模型调用）。结果用于首页「模型 Token 消耗排行」。
     * SQL 见 {@code template/mapper/AgentConversationRecordMapper.xml}。
     *
     * @return 每个模型一行的用量聚合（modelId / totalCalls / successCalls / failedCalls / totalTokensUsed / lastUsedDt）
     */
    List<AiModelUsageStatPO> aggregateModelUsageByModel();

    /**
     * 按日读时聚合 Token 消耗趋势（Prompt / Completion）。
     * <p>
     * 数据源为对话消息记录；库侧 {@code GROUP BY} 日期后返回行数至多为区间天数，
     * 供首页「Token 消耗趋势」与调用趋势同口径展示。
     * SQL 见 {@code template/mapper/AgentConversationRecordMapper.xml}。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 每个日期一行：statDate / promptTokens / completionTokens
     */
    List<Map<String, Object>> aggregateDailyTokenTrends(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
