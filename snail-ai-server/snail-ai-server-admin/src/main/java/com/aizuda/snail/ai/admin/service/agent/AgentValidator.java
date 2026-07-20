package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 智能体校验器，集中承担智能体创建/更新/会话操作前的存在性与关联资源校验，
 * 校验失败统一抛出 {@link SnailAiCommonException}，异常文案与原 {@code AgentService} 保持一致。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentValidator {

    private final AgentMapper agentMapper;
    private final AgentConversationMapper agentConversationMapper;
    private final AiModelConfigService aiModelConfigService;

    /**
     * 解析并校验智能体使用的 CHAT 模型：指定了模型 ID 则校验其存在性，未指定则回退默认 CHAT 模型
     *
     * @param chatModelId 请求指定的模型配置 ID（可为空）
     * @return 可用的 CHAT 模型配置
     */
    public AiModelConfigVO resolveChatModel(Long chatModelId) {
        AiModelConfigVO model;
        if (chatModelId != null) {
            model = aiModelConfigService.getModelConfig(chatModelId);
        } else {
            model = aiModelConfigService.getDefaultModelByType(ModelTypeEnum.CHAT.getValue());
        }
        if (model == null) {
            log.warn("智能体 CHAT 模型校验失败: chatModelId={}", chatModelId);
            throw new SnailAiCommonException("未找到可用的 CHAT 模型");
        }
        return model;
    }

    /**
     * 校验智能体存在并返回持久化对象
     *
     * @param id 智能体 ID
     * @return 智能体持久化对象
     */
    public AgentPO requireAgent(Long id) {
        AgentPO po = agentMapper.selectById(id);
        if (po == null) {
            log.warn("智能体不存在: id={}", id);
            throw new SnailAiCommonException("Agent 不存在: {}", id);
        }
        return po;
    }

    /**
     * 校验会话存在且归属指定用户（防止越权操作他人会话）并返回会话对象
     *
     * @param agentId        智能体 ID
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     * @param errorMessage   校验失败时的异常文案（区分删除/修改场景）
     * @return 会话持久化对象
     */
    public AgentConversationPO requireOwnedConversation(Long agentId, String conversationId, Long userId, String errorMessage) {
        AgentConversationPO conversation = agentConversationMapper.selectOne(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getConversationId, conversationId)
                        .eq(AgentConversationPO::getAgentId, agentId)
                        .eq(AgentConversationPO::getUserId, userId));
        if (conversation == null) {
            log.warn("会话归属校验失败: agentId={}, conversationId={}, userId={}", agentId, conversationId, userId);
            throw new SnailAiCommonException(errorMessage);
        }
        return conversation;
    }
}
