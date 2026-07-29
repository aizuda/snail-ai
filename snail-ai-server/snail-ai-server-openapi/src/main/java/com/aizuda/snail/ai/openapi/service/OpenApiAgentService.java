package com.aizuda.snail.ai.openapi.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentToolVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMcpServerMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.UserAgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentMcpServerPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.aizuda.snail.ai.persistence.agent.po.UserAgentPO;
import com.aizuda.snail.ai.persistence.mcp.mapper.McpServerMapper;
import com.aizuda.snail.ai.persistence.mcp.po.McpServerPO;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.openapi.mapper.OpenApiUserMapper;
import com.aizuda.snail.ai.persistence.openapi.po.OpenApiUserPO;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAPI Agent 查询服务
 *
 * @author opensnail
 * @since 2026-04-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenApiAgentService {

    private static final int MAX_PRESET_QUESTION_SIZE = 8;

    private final AgentMapper agentMapper;
    private final AgentMcpServerMapper agentMcpServerMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final McpServerMapper mcpServerMapper;
    private final SkillMapper skillMapper;
    private final UserAgentMapper userAgentMapper;
    private final OpenApiUserMapper openApiUserMapper;
    private final AiModelConfigMapper aiModelConfigMapper;

    /**
     * 分页查询已上架的智能体
     *
     * @param request 智能体分页查询请求
     * @return 已上架智能体分页结果
     */
    public PageResult<List<OpenApiAgentVO>> listAgents(OpenApiAgentQueryRequest request) {
        Page<AgentPO> pageParam = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<AgentPO> wrapper = new LambdaQueryWrapper<AgentPO>()
                .eq(AgentPO::getStatus, AgentStatusEnum.ACTIVE.getStatus())
                .orderByDesc(AgentPO::getUpdateDt);

        String appId = OpenApiSessionUtils.current().getAppId();
        if (StrUtil.isNotBlank(appId)) {
            wrapper.and(w -> w.isNull(AgentPO::getAppId).or().eq(AgentPO::getAppId, appId));
        }

        Page<AgentPO> page = agentMapper.selectPage(pageParam, wrapper);

        List<OpenApiAgentVO> agents = page.getRecords().stream()
                .map(agent -> toVO(agent, null))
                .collect(Collectors.toList());
        return pageResult(page, agents);
    }

    /**
     * 获取智能体详情
     *
     * @param agentId 智能体 ID
     * @return 智能体详情 VO；不存在时抛出异常
     */
    public OpenApiAgentVO getAgent(Long agentId) {
        AgentPO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new SnailAiException("Agent 不存在: " + agentId);
        }
        return toVO(agent, null);
    }

    /**
     * 查询某用户已订阅且上架的智能体列表
     *
     * @param openId 开放平台用户标识
     * @return 该用户已订阅的智能体列表
     */
    public List<OpenApiAgentVO> listUserAgents(String openId) {
        OpenApiUserPO user = getOpenApiUser(openId);
        List<UserAgentPO> relations = userAgentMapper.selectList(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, user.getPlatformUserId()));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> agentIds = relations.stream()
                .map(UserAgentPO::getAgentId)
                .collect(Collectors.toList());
        List<AgentPO> agents = agentMapper.selectList(
                new LambdaQueryWrapper<AgentPO>()
                        .in(AgentPO::getId, agentIds)
                        .eq(AgentPO::getStatus, AgentStatusEnum.ACTIVE.getStatus())
                        .orderByDesc(AgentPO::getUpdateDt));
        return agents.stream()
                .map(agent -> toVO(agent, true))
                .collect(Collectors.toList());
    }

    /**
     * 订阅智能体（已订阅时幂等返回）
     *
     * @param openId  开放平台用户标识
     * @param agentId 目标智能体 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void subscribeAgent(String openId, Long agentId) {
        OpenApiUserPO user = getOpenApiUser(openId);
        AgentPO agent = agentMapper.selectOne(
                new LambdaQueryWrapper<AgentPO>()
                        .eq(AgentPO::getId, agentId)
                        .eq(AgentPO::getStatus, AgentStatusEnum.ACTIVE.getStatus()));
        if (agent == null) {
            throw new SnailAiException("Agent 不存在或不可用: " + agentId);
        }

        Long count = userAgentMapper.selectCount(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, user.getPlatformUserId())
                        .eq(UserAgentPO::getAgentId, agentId));
        if (count > 0) {
            return;
        }

        userAgentMapper.insert(UserAgentPO.builder()
                .userId(user.getPlatformUserId())
                .agentId(agentId)
                .createDt(LocalDateTime.now())
                .build());
        log.info("OpenAPI 用户订阅智能体 openId={} agentId={}", openId, agentId);
    }

    /**
     * 取消订阅智能体
     *
     * @param openId  开放平台用户标识
     * @param agentId 目标智能体 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribeAgent(String openId, Long agentId) {
        OpenApiUserPO user = getOpenApiUser(openId);
        userAgentMapper.delete(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, user.getPlatformUserId())
                        .eq(UserAgentPO::getAgentId, agentId));
        log.info("OpenAPI 用户取消订阅智能体 openId={} agentId={}", openId, agentId);
    }

    private OpenApiUserPO getOpenApiUser(String openId) {
        String appId = OpenApiSessionUtils.current().getAppId();
        OpenApiUserPO user = openApiUserMapper.selectOne(
                new LambdaQueryWrapper<OpenApiUserPO>()
                        .eq(OpenApiUserPO::getAppId, appId)
                        .eq(OpenApiUserPO::getOpenId, openId));
        if (user == null || user.getPlatformUserId() == null) {
            throw new SnailAiException("用户不存在: " + openId);
        }
        return user;
    }

    private PageResult<List<OpenApiAgentVO>> pageResult(Page<AgentPO> page, List<OpenApiAgentVO> agents) {
        PageResult<List<OpenApiAgentVO>> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setData(agents);
        return result;
    }

    private OpenApiAgentVO toVO(AgentPO po, Boolean subscribed) {
        return OpenApiAgentVO.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .avatar(po.getAvatar())
                .greeting(po.getGreeting())
                .presetQuestions(resolvePresetQuestions(po))
                .capabilities(resolveChatModelCapabilities(po.getChatModelId()))
                .mcpEnabled(po.getMcpEnabled())
                .mcpServers(getMcpTools(po))
                .skillEnabled(po.getSkillEnabled())
                .skills(getSkillTools(po))
                .webSearchEnabled(po.getWebSearchEnabled())
                .viewCount(po.getViewCount())
                .isFeatured(po.getIsFeatured())
                .status(po.getStatus())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .subscribed(subscribed)
                .build();
    }

    private List<String> resolvePresetQuestions(AgentPO po) {
        if (StrUtil.isBlank(po.getPresetQuestions())) {
            return List.of();
        }
        try {
            List<String> parsed = JsonUtil.parseList(po.getPresetQuestions(), String.class);
            return normalizePresetQuestions(parsed);
        } catch (Exception e) {
            log.warn("解析 OpenAPI 智能体预设问题失败: agentId={}", po.getId(), e);
            return List.of();
        }
    }

    private List<String> normalizePresetQuestions(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        return questions.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(MAX_PRESET_QUESTION_SIZE)
                .collect(Collectors.toList());
    }

    private List<String> resolveChatModelCapabilities(Long chatModelId) {
        if (chatModelId == null) {
            return List.of();
        }

        AiModelConfigPO model = aiModelConfigMapper.selectById(chatModelId);
        if (model == null || StrUtil.isBlank(model.getConfigJson())) {
            return List.of();
        }

        try {
            ConfigExtAttrsDTO attrs = JsonUtil.parseObject(model.getConfigJson(), ConfigExtAttrsDTO.class);
            return normalizeCapabilities(attrs != null ? attrs.getCapabilities() : null);
        } catch (Exception e) {
            log.warn("解析 OpenAPI 智能体模型能力失败: modelConfigId={}", chatModelId, e);
            return List.of();
        }
    }

    private List<String> normalizeCapabilities(List<String> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return List.of();
        }
        return capabilities.stream()
                .filter(StrUtil::isNotBlank)
                .map(capability -> capability.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<OpenApiAgentToolVO> getMcpTools(AgentPO agent) {
        if (!Boolean.TRUE.equals(agent.getMcpEnabled())) {
            return List.of();
        }
        List<AgentMcpServerPO> relations = agentMcpServerMapper.selectList(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getAgentId, agent.getId()));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> serverIds = relations.stream()
                .map(AgentMcpServerPO::getMcpServerId)
                .collect(Collectors.toList());
        List<McpServerPO> servers = mcpServerMapper.selectByIds(serverIds);
        if (servers == null || servers.isEmpty()) {
            return List.of();
        }
        return servers.stream()
                .map(server -> OpenApiAgentToolVO.builder()
                        .id(server.getId())
                        .name(server.getName())
                        .description(server.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OpenApiAgentToolVO> getSkillTools(AgentPO agent) {
        if (!Boolean.TRUE.equals(agent.getSkillEnabled())) {
            return List.of();
        }
        List<AgentSkillPO> relations = agentSkillMapper.selectList(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getAgentId, agent.getId()));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> skillIds = relations.stream()
                .map(AgentSkillPO::getSkillId)
                .collect(Collectors.toList());
        List<SkillPO> skills = skillMapper.selectByIds(skillIds);
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .map(skill -> OpenApiAgentToolVO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
