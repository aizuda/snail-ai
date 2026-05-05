package com.aizuda.snail.ai.feature.agent.chain;


import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.skill.handle.SkillHandler;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Skill 加载：将技能元数据追加至 systemPrompt，注册 skill 工具回调，并准备远程分发的 Skill 描述符
 */
@Slf4j
@Component
@Order(70)
@RequiredArgsConstructor
public class SkillAgentChatHandler implements AgentChatHandler {

    private final SkillHandler skillHandler;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }

        if (!Boolean.TRUE.equals(ctx.getAgent().getSkillEnabled())) {
            return;
        }

        long startTime = System.currentTimeMillis();
        List<SkillPO> skills = skillHandler.getSkillsWithContentForAgent(ctx.getAgentId());
        if (skills.isEmpty()) {
            return;
        }

        List<Long> disabledIds = ctx.getDisabledSkillIds();
        if (disabledIds != null && !disabledIds.isEmpty()) {
            skills = skills.stream()
                    .filter(s -> !disabledIds.contains(s.getId()))
                    .toList();
        }

        List<ChatDispatchRequest.SkillDescriptor> descriptors = skills.stream()
                .map(s -> ChatDispatchRequest.SkillDescriptor.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .version(String.valueOf(s.getVersion()))
                        .skillPrompt(s.getSkillContent())
                        .build())
                .collect(Collectors.toList());
        ctx.setSkillDescriptors(descriptors);
    }


}
