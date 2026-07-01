package com.aizuda.snail.ai.persistence.agent.mapper;

import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 智能体-技能关联数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface AgentSkillMapper extends BaseMapper<AgentSkillPO> {

    /**
     * 按技能统计被多少智能体绑定（用于首页技能 Top 排行）。
     *
     * @return 每行包含 skillId 与 cnt
     */
    @Select("SELECT skill_id AS skillId, COUNT(*) AS cnt FROM sai_agent_skill GROUP BY skill_id")
    List<Map<String, Object>> countAgentBySkill();

    /**
     * 按智能体统计绑定的技能数（用于首页资源分配）。
     *
     * @return 每行包含 agentId 与 cnt
     */
    @Select("SELECT agent_id AS agentId, COUNT(*) AS cnt FROM sai_agent_skill GROUP BY agent_id")
    List<Map<String, Object>> countSkillByAgent();
}
