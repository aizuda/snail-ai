package com.aizuda.snail.ai.features.skill.handle;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillFileMapper;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 服务接口（基础设施层）
 * 供 features 层的工具类和 admin 层的 handler 调用，由 admin 层实现
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillHandler {

    private final SkillMapper skillMapper;
    private final AgentSkillMapper agentSkillMapper;

    /**
     * 获取智能体关联的 Skill（含 skillContent），用于对话注入
     *
     * @param agentId 智能体 ID
     * @return 该智能体关联的技能 PO 列表（含技能内容）
     */
    public List<SkillPO> getSkillsWithContentForAgent(Long agentId) {
        List<AgentSkillPO> relations = agentSkillMapper.selectList(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getAgentId, agentId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> skillIds = relations.stream()
                .map(AgentSkillPO::getSkillId)
                .collect(Collectors.toList());
        List<SkillPO> skills = skillMapper.selectByIds(skillIds);
        log.debug("加载智能体技能 agentId={} 技能数={}", agentId, skills.size());
        return skills;
    }

}
