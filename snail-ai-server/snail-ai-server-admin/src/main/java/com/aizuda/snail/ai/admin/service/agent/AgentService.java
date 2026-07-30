package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.*;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.persistence.agent.po.*;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import com.aizuda.snail.ai.admin.service.skill.SkillService;
import com.aizuda.snail.ai.admin.vo.agent.*;
import com.aizuda.snail.ai.admin.vo.memory.ConversationSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能体管理服务门面，负责智能体的增删改查、状态流转及会话管理的流程编排与持久化；
 * 参数与关联资源校验委托 {@link AgentValidator}，PO ↔ VO 装配委托 {@link AgentAssembler}。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final AgentConversationMapper agentConversationMapper;
    private final AgentConversationRecordMapper agentConversationRecordMapper;
    private final AgentUsageStatMapper agentUsageStatMapper;
    private final AiModelConfigService aiModelConfigService;
    private final McpServerService mcpServerService;
    private final SkillService skillService;
    private final AgentValidator agentValidator;
    private final AgentAssembler agentAssembler;
    private final AgentMcpServerMapper agentMcpServerMapper;

    /**
     * 分页查询智能体（支持关键字、是否精选、排序）
     *
     * @param query 查询条件
     * @return 智能体分页结果
     */
    public PageResult<List<AgentResponseVO>> page(AgentQueryVO query) {
        LambdaQueryWrapper<AgentPO> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(AgentPO::getName, query.getKeyword());
        }
        if (query.getFeatured() != null && query.getFeatured()) {
            wrapper.eq(AgentPO::getIsFeatured, true);
        }

        if ("popular".equals(query.getSort())) {
            wrapper.orderByDesc(AgentPO::getViewCount);
        } else {
            wrapper.orderByDesc(AgentPO::getCreateDt);
        }

        PageDTO<AgentPO> pageDTO = new PageDTO<>(query.getPage(), query.getSize());
        IPage<AgentPO> page = agentMapper.selectPage(pageDTO, wrapper);

        List<AgentResponseVO> records = page.getRecords().stream()
                .map(agentAssembler::toResponseVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageDTO, records);
    }

    /**
     * 创建智能体（解析默认 CHAT 模型，关联 MCP / Skill）
     *
     * @param request 智能体创建请求
     * @return 智能体响应视图
     */
    public AgentResponseVO create(AgentRequestVO request) {
        Long userId = UserSessionUtils.currentUserSession().getId();

        // 获取默认 CHAT 模型
        AiModelConfigVO defaultModel = agentValidator.resolveChatModel(request.getChatModelId());

        AgentPO agent = agentAssembler.buildCreatePO(request, userId, defaultModel);

        agentMapper.insert(agent);
        Long agentId = agent.getId();

        if (agentId == null) {
            throw new SnailAiCommonException("创建智能体失败：无法获取生成的ID");
        }

        // 关联 MCP 服务
        if (request.getMcpServerIds() != null && !request.getMcpServerIds().isEmpty()) {
            mcpServerService.updateAgentMcpServers(agentId, request.getMcpServerIds());
        }

        // 关联 Skill
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skillService.updateAgentSkills(agentId, request.getSkillIds());
        }

        // 重新查询以确保返回最新的数据
        AgentPO savedAgent = agentMapper.selectById(agentId);
        if (savedAgent == null) {
            throw new SnailAiCommonException("创建智能体失败：保存后无法查询到数据");
        }

        log.info("创建智能体成功 id={} name={} chatModelId={}", agentId, agent.getName(), defaultModel.getId());
        return agentAssembler.toResponseVO(savedAgent);
    }

    /**
     * 按 ID 查询智能体详情（并自增浏览次数）
     *
     * @param id 智能体 ID
     * @return 智能体响应视图
     */
    public AgentResponseVO getById(Long id) {
        AgentPO po = agentValidator.requireAgent(id);
        // 增加浏览次数
        AgentPO update = new AgentPO();
        update.setId(id);
        update.setViewCount(po.getViewCount() != null ? po.getViewCount() + 1 : 1);
        agentMapper.updateById(update);

        return agentAssembler.toResponseVO(po);
    }

    /**
     * 更新智能体配置（按非空字段增量更新，并同步 MCP / Skill 关联）
     *
     * @param id      智能体 ID
     * @param request 智能体更新请求
     * @return 智能体响应视图
     */
    public AgentResponseVO update(Long id, AgentRequestVO request) {
        AgentPO po = agentValidator.requireAgent(id);

        agentAssembler.applyUpdateRequest(po, request);

        agentMapper.updateById(po);

        // 更新 MCP 服务关联
        if (request.getMcpServerIds() != null) {
            mcpServerService.updateAgentMcpServers(id, request.getMcpServerIds());
        }

        // 更新 Skill 关联
        if (request.getSkillIds() != null) {
            skillService.updateAgentSkills(id, request.getSkillIds());
        }

        log.info("更新智能体成功 id={}", id);
        return agentAssembler.toResponseVO(po);
    }

    /**
     * 删除智能体及其会话、记录与用量统计
     *
     * @param id 智能体 ID
     */
    @Transactional
    public void delete(Long id) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            return;
        }

        // 删除关联数据
        agentConversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>().eq(AgentConversationRecordPO::getAgentId, id));
        agentConversationMapper.delete(
                new LambdaQueryWrapper<AgentConversationPO>().eq(AgentConversationPO::getAgentId, id));
        agentUsageStatMapper.delete(
                new LambdaQueryWrapper<AgentUsageStatPO>().eq(AgentUsageStatPO::getAgentId, id));
        agentMcpServerMapper.delete(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getAgentId, id));
        agentMapper.deleteById(id);
        log.info("删除智能体成功 id={} name={}", id, po.getName());
    }

    /**
     * 删除单个对话及其关联数据（不删除长期记忆 {@code sai_memory_conversation}）
     *
     * @param agentId        智能体 ID
     * @param conversationId 会话 ID
     */
    @Transactional
    public void deleteConversation(Long agentId, String conversationId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        AgentConversationPO conversation = agentValidator.requireOwnedConversation(
                agentId, conversationId, userId, "对话不存在或无权限删除");

        agentConversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getConversationId, conversationId)
                        .eq(AgentConversationRecordPO::getAgentId, agentId)
                        .eq(AgentConversationRecordPO::getUserId, userId));
        agentConversationMapper.deleteById(conversation.getId());
        log.info("删除对话成功: agentId={}, conversationId={}, userId={}", agentId, conversationId, userId);
    }

    /**
     * 清空当前用户在指定 Agent 下的全部对话（不删除长期记忆 {@code sai_memory_conversation}）
     *
     * @param agentId 智能体 ID
     */
    @Transactional
    public void clearConversations(Long agentId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        agentConversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, agentId)
                        .eq(AgentConversationRecordPO::getUserId, userId));
        agentConversationMapper.delete(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getAgentId, agentId)
                        .eq(AgentConversationPO::getUserId, userId));
        log.info("清空对话成功: agentId={}, userId={}", agentId, userId);
    }

    /**
     * 更新会话标题
     *
     * @param agentId        智能体 ID
     * @param conversationId 会话 ID
     * @param title          新标题
     */
    public void updateConversationTitle(Long agentId, String conversationId, String title) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        AgentConversationPO conversation = agentValidator.requireOwnedConversation(
                agentId, conversationId, userId, "对话不存在或无权限修改");
        conversation.setTitle(title);
        conversation.setUpdateDt(LocalDateTime.now());
        agentConversationMapper.updateById(conversation);
        log.info("更新对话标题成功: agentId={}, conversationId={}, title={}", agentId, conversationId, title);
    }

    /**
     * 批量删除指定会话（逐个调用 {@link #deleteConversation}，受本人权限约束）
     *
     * @param agentId         智能体 ID
     * @param conversationIds 会话 ID 列表
     */
    @Transactional
    public void batchDeleteConversations(Long agentId, List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return;
        }
        for (String conversationId : conversationIds) {
            deleteConversation(agentId, conversationId);
        }
    }

    /**
     * 分页查询当前用户在指定 Agent 下的会话列表（{@code sai_agent_conversation} + 记录表聚合统计）
     *
     * @param agentId 智能体 ID
     * @param query   分页与时间范围查询条件
     * @return 会话摘要分页结果
     */
    public PageResult<List<ConversationSummaryVO>> listConversations(Long agentId, AgentConversationQueryVO query) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        String userName = UserSessionUtils.currentUserSession().getUsername();

        LambdaQueryWrapper<AgentConversationPO> wrapper = new LambdaQueryWrapper<AgentConversationPO>()
                .eq(AgentConversationPO::getAgentId, agentId)
                .eq(AgentConversationPO::getUserId, userId);

        if (ObjUtil.isNotNull(query.getStartDt()) && ObjUtil.isNotNull(query.getEndDt())) {
            wrapper.between(AgentConversationPO::getCreateDt, query.getStartDt(), query.getEndDt());
        } else if (StrUtil.isNotBlank(query.getStart()) && StrUtil.isNotBlank(query.getEnd())) {
            LocalDate start = LocalDate.parse(query.getStart().trim());
            LocalDate end = LocalDate.parse(query.getEnd().trim());
            if (start.isAfter(end)) {
                LocalDate t = start;
                start = end;
                end = t;
            }
            wrapper.ge(AgentConversationPO::getCreateDt, start.atStartOfDay())
                    .le(AgentConversationPO::getCreateDt, LocalDateTime.of(end, LocalTime.MAX));
        }

        wrapper.orderByDesc(AgentConversationPO::getUpdateDt);

        Page<AgentConversationPO> pageParam = new Page<>(query.getPage(), query.getSize());
        Page<AgentConversationPO> page = agentConversationMapper.selectPage(pageParam, wrapper);

        List<String> convIds = page.getRecords().stream()
                .map(AgentConversationPO::getConversationId)
                .collect(Collectors.toList());

        List<AgentConversationRecordPO> records = List.of();
        if (!convIds.isEmpty()) {
            records = agentConversationRecordMapper.selectList(
                    new LambdaQueryWrapper<AgentConversationRecordPO>()
                            .select(AgentConversationRecordPO::getConversationId, AgentConversationRecordPO::getCreateDt)
                            .eq(AgentConversationRecordPO::getAgentId, agentId)
                            .eq(AgentConversationRecordPO::getUserId, userId)
                            .in(AgentConversationRecordPO::getConversationId, convIds));
        }

        List<ConversationSummaryVO> rows = agentAssembler.toConversationSummaries(page.getRecords(), records, userName);

        return new PageResult<>(page, rows);
    }

    /**
     * 获取可用的 CHAT 类型模型列表
     *
     * @return 模型选项列表（含 value/label/id）
     */
    public List<Map<String, Object>> getChatModels() {
        List<AiModelConfigVO> models = aiModelConfigService.getModelsByType(ModelTypeEnum.CHAT.getValue());
        return agentAssembler.toChatModelOptions(models);
    }

    /**
     * 将智能体 PO 转换为响应视图（含模型名、MCP / Skill 关联与预设问题），委托 {@link AgentAssembler}
     *
     * @param po 智能体持久化对象
     * @return 智能体响应视图
     */
    public AgentResponseVO toResponseVO(AgentPO po) {
        return agentAssembler.toResponseVO(po);
    }

}
