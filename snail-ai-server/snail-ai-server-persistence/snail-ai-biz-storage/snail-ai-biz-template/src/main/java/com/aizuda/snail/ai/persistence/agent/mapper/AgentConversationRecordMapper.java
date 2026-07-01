package com.aizuda.snail.ai.persistence.agent.mapper;

import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelUsageStatPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
     *
     * @return 每个模型一行的用量聚合（modelId / totalCalls / successCalls / failedCalls / totalTokensUsed / lastUsedDt）
     */
    @Select("""
            SELECT a.chat_model_id AS model_id,
                   COUNT(*) AS total_calls,
                   SUM(CASE WHEN cr.status < 0 THEN 0 ELSE 1 END) AS success_calls,
                   SUM(CASE WHEN cr.status < 0 THEN 1 ELSE 0 END) AS failed_calls,
                   COALESCE(SUM(cr.input_tokens), 0) + COALESCE(SUM(cr.output_tokens), 0) AS total_tokens_used,
                   MAX(cr.create_dt) AS last_used_dt
            FROM sai_agent_conversation_record cr
            JOIN sai_agent a ON cr.agent_id = a.id
            WHERE a.chat_model_id IS NOT NULL
              AND (cr.input_tokens > 0 OR cr.output_tokens > 0)
            GROUP BY a.chat_model_id
            """)
    List<AiModelUsageStatPO> aggregateModelUsageByModel();
}
