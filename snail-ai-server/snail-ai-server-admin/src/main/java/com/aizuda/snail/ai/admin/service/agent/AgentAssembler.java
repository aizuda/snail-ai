package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.admin.service.skill.SkillService;
import com.aizuda.snail.ai.admin.vo.agent.AgentRequestVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentResponseVO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.vo.memory.ConversationSummaryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
import com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum;
import com.aizuda.snail.ai.common.enums.agent.RagCallModeEnum;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aizuda.snail.ai.common.constants.SystemConstants.YYYY_MM_DD_HH_MM_SS;

/**
 * 智能体装配器，集中承担 PO ↔ VO 转换与关联信息组装：响应视图装配（含模型名、MCP / Skill 绑定）、
 * 创建/更新请求到持久化对象的字段装配、会话摘要聚合、预设问题规范化等，不承担任何持久化写操作。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentAssembler {

    private final AiModelConfigService aiModelConfigService;
    private final McpServerService mcpServerService;
    private final SkillService skillService;

    /**
     * 将智能体 PO 转换为响应视图（含模型名、MCP / Skill 关联与预设问题）
     *
     * @param po 智能体持久化对象
     * @return 智能体响应视图
     */
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
                .ragIds(StrUtil.isBlank(po.getRagIds()) ? null : po.getRagIds())
                .ragCallMode(po.getRagCallMode())
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

    /**
     * 依据创建请求装配待入库的智能体 PO（默认 ACTIVE 状态、浏览数 0、非精选）
     *
     * @param request   智能体创建请求
     * @param userId    创建人用户 ID
     * @param chatModel 已解析的 CHAT 模型配置
     * @return 待插入的智能体持久化对象
     */
    public AgentPO buildCreatePO(AgentRequestVO request, Long userId, AiModelConfigVO chatModel) {
        List<String> normalizedQuestions = normalizePresetQuestions(request.getPresetQuestions());

        return AgentPO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .instruction(request.getInstruction())
                .greeting(request.getGreeting() != null ? request.getGreeting().trim() : null)
                .presetQuestions(normalizedQuestions.isEmpty() ? null : JsonUtil.toJsonString(normalizedQuestions))
                .avatar(request.getAvatar())
                .chatModelId(chatModel.getId())
                .creatorId(userId)
                .ragIds(request.getRagIds())
                .ragCallMode(request.getRagCallMode() != null ? request.getRagCallMode() : RagCallModeEnum.SMART.getMode())
                .status(AgentStatusEnum.ACTIVE.getStatus())
                .viewCount(0)
                .mcpEnabled(Boolean.TRUE.equals(request.getMcpEnabled()))
                .skillEnabled(Boolean.TRUE.equals(request.getSkillEnabled()))
                .webSearchEnabled(Boolean.TRUE.equals(request.getWebSearchEnabled()))
                .ragEnabled(Boolean.TRUE.equals(request.getRagEnabled()))
                .memoryEnabled(Boolean.TRUE.equals(request.getMemoryEnabled()))
                .shortTermMemorySize(request.getShortTermMemorySize())
                .isFeatured(false)
                .appId(request.getAppId())
                .build();
    }

    /**
     * 将更新请求按非空字段增量合并到智能体 PO 上（关闭 RAG 时清空 ragIds / ragCallMode，appId 允许置空）
     *
     * @param po      待更新的智能体持久化对象
     * @param request 智能体更新请求
     */
    public void applyUpdateRequest(AgentPO po, AgentRequestVO request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            po.setAvatar(request.getAvatar());
        }
        if (request.getInstruction() != null) {
            po.setInstruction(request.getInstruction());
        }

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
        if (request.getChatModelId() != null) {
            po.setChatModelId(request.getChatModelId());
        }
        if (request.getMcpEnabled() != null) {
            po.setMcpEnabled(request.getMcpEnabled());
        }
        if (request.getSkillEnabled() != null) {
            po.setSkillEnabled(request.getSkillEnabled());
        }
        if (request.getWebSearchEnabled() != null) {
            po.setWebSearchEnabled(request.getWebSearchEnabled());
        }
        if (request.getRagEnabled() != null) {
            po.setRagEnabled(request.getRagEnabled());
        }
        if (request.getMemoryEnabled() != null) {
            po.setMemoryEnabled(request.getMemoryEnabled());
        }
        if (request.getShortTermMemorySize() != null) {
            po.setShortTermMemorySize(request.getShortTermMemorySize());
        }
        if (request.getIsFeatured() != null) {
            po.setIsFeatured(request.getIsFeatured());
        }
        if (request.getRagIds() != null) {
            po.setRagIds(request.getRagIds());
        }
        if (request.getRagCallMode() != null) {
            po.setRagCallMode(request.getRagCallMode());
        }
        if (Boolean.FALSE.equals(request.getRagEnabled())) {
            po.setRagIds(null);
            po.setRagCallMode(null);
        }
        // appId 允许设为 null（清空 = 本地执行）
        po.setAppId(request.getAppId());
    }

    /**
     * 将 CHAT 模型配置列表装配为前端下拉选项（value/label/id/capabilities）
     *
     * @param models CHAT 模型配置列表
     * @return 模型选项列表
     */
    public List<Map<String, Object>> toChatModelOptions(List<AiModelConfigVO> models) {
        return models.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", m.getModelKey());
            map.put("label", m.getModelName());
            map.put("id", m.getId());
            map.put("capabilities", m.getConfigJson() == null || m.getConfigJson().getCapabilities() == null
                    ? List.of()
                    : m.getConfigJson().getCapabilities());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 将会话列表与消息记录聚合装配为会话摘要视图（消息数、最后消息时间兜底取会话更新时间）
     *
     * @param conversations 当前页会话列表
     * @param records       会话消息记录（仅含 conversationId / createDt 字段）
     * @param userName      当前用户名
     * @return 会话摘要视图列表
     */
    public List<ConversationSummaryVO> toConversationSummaries(List<AgentConversationPO> conversations,
                                                               List<AgentConversationRecordPO> records,
                                                               String userName) {
        Map<String, ConversationStats> statsByConvId = new HashMap<>();
        for (AgentConversationRecordPO record : records) {
            String conversationId = record.getConversationId();
            if (conversationId == null) {
                continue;
            }
            statsByConvId.merge(conversationId,
                    new ConversationStats(1, record.getCreateDt()),
                    AgentAssembler::mergeConversationStats);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
        return conversations.stream().map(conv -> {
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
