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

/**
 * 用户智能体服务，管理当前用户已订阅/可见的智能体列表。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAgentService {

    private final UserAgentMapper userAgentMapper;
    private final AgentMapper agentMapper;
    private final AgentService agentService;

    /**
     * 获取当前用户已订阅的智能体列表
     *
     * @return 已订阅智能体视图列表
     */
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

    /**
     * 获取智能体市场列表（全部已激活智能体，并标记当前用户是否已订阅）
     *
     * @return 智能体视图列表（含 subscribed 标记）
     */
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

    /**
     * 当前用户订阅指定智能体（已订阅则忽略）
     *
     * @param agentId 智能体 ID
     */
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
        log.info("用户订阅智能体 userId={} agentId={}", userId, agentId);
    }

    /**
     * 当前用户取消订阅指定智能体
     *
     * @param agentId 智能体 ID
     */
    public void unsubscribe(Long agentId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        userAgentMapper.delete(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, userId)
                        .eq(UserAgentPO::getAgentId, agentId));
        log.info("用户取消订阅智能体 userId={} agentId={}", userId, agentId);
    }
}
