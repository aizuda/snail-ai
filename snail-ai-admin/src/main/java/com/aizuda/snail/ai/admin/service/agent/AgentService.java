package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.*;
import com.aizuda.snail.ai.persistence.memory.mapper.ConversationSummaryMapper;
import com.aizuda.snail.ai.persistence.memory.po.ConversationSummaryPO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.persistence.agent.po.*;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.service.skill.SkillService;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aizuda.snail.ai.common.constants.SystemConstants.YYYY_MM_DD_HH_MM_SS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Long RAG_ID_NONE = 0L;

    private final AgentMapper agentMapper;
    private final AgentConversationMapper agentConversationMapper;
    private final AgentConversationRecordMapper agentConversationRecordMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final AgentUsageStatMapper agentUsageStatMapper;
    private final AiModelConfigService aiModelConfigService;
    private final McpServerService mcpServerService;
    private final SkillService skillService;

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
                .map(this::toResponseVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageDTO, records);
    }

    public AgentResponseVO getById(Long id) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            throw new SnailAiCommonException("Agent 不存在: {}", id);
        }
        // 增加浏览次数
        AgentPO update = new AgentPO();
        update.setId(id);
        update.setViewCount(po.getViewCount() != null ? po.getViewCount() + 1 : 1);
        agentMapper.updateById(update);

        return toResponseVO(po);
    }

    public AgentResponseVO update(Long id, AgentRequestVO request) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            throw new SnailAiCommonException("Agent 不存在: {}", id);
        }

        if (request.getName() != null) po.setName(request.getName());
        if (request.getDescription() != null) po.setDescription(request.getDescription());
        if (request.getAvatar() != null) po.setAvatar(request.getAvatar());
        if (request.getInstruction() != null) po.setInstruction(request.getInstruction());
        
        // greeting 和 presetQuestions 独立处理，不再同步
        if (request.getGreeting() != null) {
            po.setGreeting(request.getGreeting().trim());
        }
        
        if (request.getPresetQuestions() != null) {
            List<String> normalizedPresetQuestions = normalizePresetQuestions(request.getPresetQuestions());
            if (normalizedPresetQuestions.isEmpty()) {
                po.setPresetQuestions(null);
            } else {
                po.setPresetQuestions(JsonUtil.toJsonString(normalizedPresetQuestions));
            }
        }
        if (request.getChatModelId() != null) po.setChatModelId(request.getChatModelId());
        if (request.getMcpEnabled() != null) po.setMcpEnabled(request.getMcpEnabled());
        if (request.getSkillEnabled() != null) po.setSkillEnabled(request.getSkillEnabled());
        if (request.getWebSearchEnabled() != null) po.setWebSearchEnabled(request.getWebSearchEnabled());
        if (request.getRagEnabled() != null) po.setRagEnabled(request.getRagEnabled());
        if (request.getMemoryEnabled() != null) po.setMemoryEnabled(request.getMemoryEnabled());
        if (request.getShortTermMemorySize() != null) po.setShortTermMemorySize(request.getShortTermMemorySize());
        if (request.getIsFeatured() != null) po.setIsFeatured(request.getIsFeatured());
        if (request.getRagId() != null) {
            po.setRagId(request.getRagId());
        }
        if (Boolean.FALSE.equals(request.getRagEnabled())) {
            po.setRagId(RAG_ID_NONE);
        }
        // appId 允许设为 null（清空 = 本地执行）
        po.setAppId(request.getAppId());

        agentMapper.updateById(po);

        // 更新 MCP 服务关联
        if (request.getMcpServerIds() != null) {
            mcpServerService.updateAgentMcpServers(id, request.getMcpServerIds());
        }

        // 更新 Skill 关联
        if (request.getSkillIds() != null) {
            skillService.updateAgentSkills(id, request.getSkillIds());
        }

        return toResponseVO(po);
    }

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
        agentMapper.deleteById(id);
    }

    /**
     * 删除单个对话及其关联数据（不删除长期记忆 {@code snail_ai_conversation_memory}）
     */
    @Transactional
    public void deleteConversation(Long agentId, String conversationId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        AgentConversationPO conversation = agentConversationMapper.selectOne(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getConversationId, conversationId)
                        .eq(AgentConversationPO::getAgentId, agentId)
                        .eq(AgentConversationPO::getUserId, userId));
        if (conversation == null) {
            throw new SnailAiCommonException("对话不存在或无权限删除");
        }

        agentConversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getConversationId, conversationId)
                        .eq(AgentConversationRecordPO::getAgentId, agentId));
        conversationSummaryMapper.delete(
                new LambdaQueryWrapper<ConversationSummaryPO>()
                        .eq(ConversationSummaryPO::getConversationId, conversationId)
                        .eq(ConversationSummaryPO::getAgentId, agentId));
        agentConversationMapper.deleteById(conversation.getId());
        log.info("删除对话成功: agentId={}, conversationId={}, userId={}", agentId, conversationId, userId);
    }

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
     * 分页查询当前用户在指定 Agent 下的会话列表（{@code snail_ai_agent_conversation} + 记录表聚合统计）
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

        Map<String, ConversationStats> statsByConvId = new HashMap<>();
        if (!convIds.isEmpty()) {
            List<AgentConversationRecordPO> records = agentConversationRecordMapper.selectList(
                    new LambdaQueryWrapper<AgentConversationRecordPO>()
                            .select(AgentConversationRecordPO::getConversationId, AgentConversationRecordPO::getCreateDt)
                            .eq(AgentConversationRecordPO::getAgentId, agentId)
                            .eq(AgentConversationRecordPO::getUserId, userId)
                            .in(AgentConversationRecordPO::getConversationId, convIds));
            for (AgentConversationRecordPO record : records) {
                String conversationId = record.getConversationId();
                if (conversationId == null) {
                    continue;
                }
                statsByConvId.merge(conversationId,
                        new ConversationStats(1, record.getCreateDt()),
                        AgentService::mergeConversationStats);
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
        List<ConversationSummaryVO> rows = page.getRecords().stream().map(conv -> {
            ConversationStats st = statsByConvId.get(conv.getConversationId());
            int messageCount = st != null ? st.messageCount() : 0;
            LocalDateTime lastMsg = st != null && st.lastMessageDt() != null
                    ? st.lastMessageDt()
                    : conv.getUpdateDt();
            return ConversationSummaryVO.builder()
                    .conversationId(conv.getConversationId())
                    .title(conv.getTitle())
                    .userName(userName)
                    .messageCount(messageCount)
                    .toolCallCount(0)
                    .createDt(conv.getCreateDt() != null ? conv.getCreateDt().format(dtf) : null)
                    .lastMessageDt(lastMsg != null ? lastMsg.format(dtf) : null)
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(page, rows);
    }

    private record ConversationStats(int messageCount, LocalDateTime lastMessageDt) {
    }

    private static ConversationStats mergeConversationStats(ConversationStats left, ConversationStats right) {
        return new ConversationStats(
                left.messageCount() + right.messageCount(),
                laterOf(left.lastMessageDt(), right.lastMessageDt()));
    }

    private static LocalDateTime laterOf(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 获取可用的 CHAT 类型模型列表
     */
    public List<Map<String, Object>> getChatModels() {
        List<AiModelConfigVO> models = aiModelConfigService.getModelsByType(ModelTypeEnum.CHAT.getValue());
        return models.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", m.getModelKey());
            map.put("label", m.getModelName());
            map.put("id", m.getId());
            return map;
        }).collect(Collectors.toList());
    }

    public AgentResponseVO toResponseVO(AgentPO po) {
        String chatModelName = null;
        if (po.getChatModelId() != null) {
            AiModelConfigVO modelConfig = aiModelConfigService.getModelConfig(po.getChatModelId());
            if (modelConfig != null) {
                chatModelName = modelConfig.getModelKey();
            }
        }

        // 查询关联的 MCP 服务
        List<McpServerResponseVO> mcpServers = null;
        if (Boolean.TRUE.equals(po.getMcpEnabled())) {
            mcpServers = mcpServerService.getByAgentId(po.getId());
        }

        // 查询关联的 Skill
        List<SkillResponseVO> skills = null;
        if (Boolean.TRUE.equals(po.getSkillEnabled())) {
            skills = skillService.getByAgentId(po.getId());
        }

        List<String> presetQuestions = resolvePresetQuestions(po);

        return AgentResponseVO.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .avatar(po.getAvatar())
                .instruction(po.getInstruction())
                .greeting(po.getGreeting())
                .presetQuestions(presetQuestions)
                .chatModelId(po.getChatModelId())
                .chatModel(chatModelName)
                .mcpEnabled(po.getMcpEnabled())
                .mcpServers(mcpServers)
                .skillEnabled(po.getSkillEnabled())
                .skills(skills)
                .webSearchEnabled(po.getWebSearchEnabled())
                .ragEnabled(po.getRagEnabled())
                .memoryEnabled(po.getMemoryEnabled())
                .ragId(RAG_ID_NONE.equals(po.getRagId()) ? null : po.getRagId())
                .shortTermMemorySize(po.getShortTermMemorySize())
                .creator(null)
                .viewCount(po.getViewCount())
                .isFeatured(po.getIsFeatured())
                .status(po.getStatus())
                .appId(po.getAppId())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    private List<String> resolvePresetQuestions(AgentPO po) {
        if (StrUtil.isNotBlank(po.getPresetQuestions())) {
            try {
                List<String> parsed = JsonUtil.parseList(po.getPresetQuestions(), String.class);
                List<String> normalized = normalizePresetQuestions(parsed);
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            } catch (Exception e) {
                log.warn("解析智能体预设问题失败: agentId={}", po.getId(), e);
            }
        }
        // 不再从 greeting 回源，保持独立性
        return List.of();
    }

    private List<String> normalizePresetQuestions(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        return questions.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
    }

}
