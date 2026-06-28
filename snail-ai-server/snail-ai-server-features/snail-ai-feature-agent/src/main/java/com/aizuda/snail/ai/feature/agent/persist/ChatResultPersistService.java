package com.aizuda.snail.ai.feature.agent.persist;

import com.aizuda.snail.ai.common.enums.agent.StatusEnum;
import com.aizuda.snail.ai.memory.store.ShortTermMemoryStore;
import com.aizuda.snail.ai.persistence.agent.enums.ConversationRoleEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentUsageStatMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentUsageStatPO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chat 流结束后的持久化：助手消息、短期记忆窗口、用量统计。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatResultPersistService {

    private final AgentConversationRecordMapper recordMapper;
    private final AgentUsageStatMapper usageStatMapper;
    private final ShortTermMemoryStore shortTermMemoryStore;

    /**
     * 异步持久化对话结果：在虚拟线程中执行，避免阻塞流式响应主链路；失败仅记录日志、不向上抛出。
     */
    public void persistAsync(ChatResultPersistCommand cmd) {
        Thread.startVirtualThread(() -> {
            try {
                persistAll(cmd);
            } catch (Exception e) {
                log.error("Chat result persistence failed: conversationId={}", cmd.getConversationId(), e);
            }
        });
    }

    /**
     * 串行持久化助手消息、短期记忆窗口与用量统计；任一步失败即抛出，由调用方统一记录。
     */
    private void persistAll(ChatResultPersistCommand cmd) {
        log.debug("persistAll start: conversationId={}, inputTokens={}, outputTokens={}, cacheTokens={}",
                cmd.getConversationId(), cmd.getInputTokens(), cmd.getOutputTokens(), cmd.getCacheTokens());

        // 1. 插入对话记录
        try {
            int rows = recordMapper.insert(AgentConversationRecordPO.builder()
                    .agentId(cmd.getAgentId()).conversationId(cmd.getConversationId()).userId(cmd.getUserId())
                    .role(ConversationRoleEnum.ASSISTANT.getValue())
                    .content(cmd.getFullText())
                    .thinking(cmd.getThinkingText() != null && !cmd.getThinkingText().isEmpty()
                            ? cmd.getThinkingText() : null)
                    .status(StatusEnum.RUNNING.getValue())
                    .inputTokens(cmd.getInputTokens())
                    .outputTokens(cmd.getOutputTokens())
                    .cacheTokens(cmd.getCacheTokens())
                    .build());
            log.debug("persistAll insert done: conversationId={}, rows={}", cmd.getConversationId(), rows);
        } catch (Exception e) {
            log.error("persistAll insert FAILED: conversationId={}, error={}", cmd.getConversationId(), e.getMessage(), e);
            throw e;
        }

        // 2. 短期记忆
        try {
            if (!Boolean.FALSE.equals(cmd.getMemoryEnabled())) {
                shortTermMemoryStore.append(cmd.getConversationId(), ConversationRoleEnum.ASSISTANT.getValue(),
                        cmd.getFullText(), cmd.getShortTermWindow());
                log.debug("persistAll memory done: conversationId={}", cmd.getConversationId());
            }
        } catch (Exception e) {
            log.error("persistAll memory FAILED: conversationId={}, error={}", cmd.getConversationId(), e.getMessage(), e);
            throw e;
        }

        // 3. 用量统计
        try {
            updateUsageStat(cmd.getAgentId(), cmd.getUserId());
            log.debug("persistAll usageStat done: conversationId={}", cmd.getConversationId());
        } catch (Exception e) {
            log.error("persistAll usageStat FAILED: conversationId={}, error={}", cmd.getConversationId(), e.getMessage(), e);
            throw e;
        }

        log.debug("persistAll ALL done: conversationId={}", cmd.getConversationId());
    }

    /**
     * 更新当日用量统计：优先自增已有记录，无记录则插入；并发插入冲突时回退为自增。
     */
    private void updateUsageStat(Long agentId, Long userId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            int updated = incrementUsageStat(agentId, userId, today, now);
            if (updated != 0) {
                return;
            }
            try {
                usageStatMapper.insert(AgentUsageStatPO.builder()
                        .agentId(agentId).userId(userId)
                        .statDate(today).messageCount(1).conversationCount(1)
                        .createDt(now).updateDt(now)
                        .build());
            } catch (DuplicateKeyException e) {
                incrementUsageStat(agentId, userId, today, now);
            }
        } catch (Exception e) {
            log.warn("Failed to update usage stat", e);
        }
    }

    /**
     * 自增指定 Agent / 用户 / 当日的消息计数，返回受影响行数（0 表示当日记录尚不存在）。
     */
    private int incrementUsageStat(Long agentId, Long userId, LocalDate today, LocalDateTime now) {
        return usageStatMapper.update(null, new LambdaUpdateWrapper<AgentUsageStatPO>()
                .eq(AgentUsageStatPO::getAgentId, agentId)
                .eq(AgentUsageStatPO::getUserId, userId)
                .eq(AgentUsageStatPO::getStatDate, today)
                .setSql("message_count = message_count + 1")
                .set(AgentUsageStatPO::getUpdateDt, now));
    }

}
