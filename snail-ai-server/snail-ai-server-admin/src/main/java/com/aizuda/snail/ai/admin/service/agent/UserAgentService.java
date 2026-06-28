package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.UserAgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.UserAgentPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.admin.vo.agent.AgentResponseVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAgentService {

    private final UserAgentMapper userAgentMapper;
    private final AgentMapper agentMapper;
    private final AgentService agentService;

    public List<AgentResponseVO> getMyAgents() {
        Long userId = UserSessionUtils.currentUserSession().getId();
        List<UserAgentPO> relations = userAgentMapper.selectList(
                new LambdaQueryWrapper<UserAgentPO>().eq(UserAgentPO::getUserId, userId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> agentIds = relations.stream()
                .map(UserAgentPO::getAgentId)
                .collect(Collectors.toList());
        List<AgentPO> agents = agentMapper.selectByIds(agentIds);
        return agents.stream().map(agentService::toResponseVO).collect(Collectors.toList());
    }

    public List<AgentResponseVO> getMarket() {
        Long userId = UserSessionUtils.currentUserSession().getId();
        Set<Long> subscribedIds = userAgentMapper.selectList(
                        new LambdaQueryWrapper<UserAgentPO>().eq(UserAgentPO::getUserId, userId))
                .stream().map(UserAgentPO::getAgentId).collect(Collectors.toSet());

        List<AgentPO> agents = agentMapper.selectList(
                new LambdaQueryWrapper<AgentPO>()
                        .eq(AgentPO::getStatus, AgentStatusEnum.ACTIVE.getStatus())
                        .orderByDesc(AgentPO::getCreateDt));

        return agents.stream().map(po -> {
            AgentResponseVO vo = agentService.toResponseVO(po);
            vo.setSubscribed(subscribedIds.contains(po.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    public void subscribe(Long agentId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        Long count = userAgentMapper.selectCount(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, userId)
                        .eq(UserAgentPO::getAgentId, agentId));
        if (count > 0) {
            return;
        }
        userAgentMapper.insert(UserAgentPO.builder()
                .userId(userId)
                .agentId(agentId)
                .build());
    }

    public void unsubscribe(Long agentId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        userAgentMapper.delete(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, userId)
                        .eq(UserAgentPO::getAgentId, agentId));
    }
}
