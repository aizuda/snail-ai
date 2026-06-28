package com.aizuda.snail.ai.features.skill.handle;

import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Skill 服务接口（基础设施层） 供 features 层的工具类和 admin 层的 handler 调用，由 admin 层实现 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillHandler {

    private final SkillMapper skillMapper;
    private final AgentSkillMapper agentSkillMapper;

    /** 获取智能体关联的 Skill（含 skillContent），用于对话注入 */
    public List<SkillPO> getSkillsWithContentForAgent(Long agentId) {
        List<AgentSkillPO> relations =
                agentSkillMapper.selectList(
                        new LambdaQueryWrapper<AgentSkillPO>()
                                .eq(AgentSkillPO::getAgentId, agentId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> skillIds =
                relations.stream().map(AgentSkillPO::getSkillId).collect(Collectors.toList());
        return skillMapper.selectByIds(skillIds);
    }
}
