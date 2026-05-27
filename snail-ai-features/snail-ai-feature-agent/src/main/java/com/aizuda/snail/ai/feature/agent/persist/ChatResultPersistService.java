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
import java.util.concurrent.CompletableFuture;

/**
 * Chat 流结束后的持久化：助手消息、短期记忆窗口、用量统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatResultPersistService {

    private final AgentConversationRecordMapper recordMapper;
    private final AgentUsageStatMapper usageStatMapper;
    private final ShortTermMemoryStore shortTermMemoryStore;

    public void persistAsync(ChatResultPersistCommand cmd) {
        CompletableFuture.runAsync(() -> {
            try {
                persistAll(cmd);
            } catch (Exception e) {
                log.error("Chat result persistence failed: conversationId={}", cmd.getConversationId(), e);
            }
        });
    }

    private void persistAll(ChatResultPersistCommand cmd) {
        recordMapper.insert(AgentConversationRecordPO.builder()
                .agentId(cmd.getAgentId()).conversationId(cmd.getConversationId()).userId(cmd.getUserId())
                .role(ConversationRoleEnum.ASSISTANT.getValue())
                .content(cmd.getFullText())
                .thinking(cmd.getThinkingText() != null && !cmd.getThinkingText().isEmpty()
                        ? cmd.getThinkingText() : null)
                .status(StatusEnum.RUNNING.getValue())
                .build());

        if (!Boolean.FALSE.equals(cmd.getMemoryEnabled())) {
            shortTermMemoryStore.append(cmd.getConversationId(), ConversationRoleEnum.ASSISTANT.getValue(),
                    cmd.getFullText(), cmd.getShortTermWindow());
        }

        log.debug("Assistant message persisted: conversationId={}, length={}", cmd.getConversationId(),
                cmd.getFullText() != null ? cmd.getFullText().length() : 0);

        updateUsageStat(cmd.getAgentId(), cmd.getUserId(), cmd.getUserName());
    }

    private void updateUsageStat(Long agentId, Long userId, String userName) {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            int updated = incrementUsageStat(agentId, userId, today, now);
            if (updated == 0) {
                try {
                    usageStatMapper.insert(AgentUsageStatPO.builder()
                            .agentId(agentId).userId(userId).userName(userName)
                            .statDate(today).messageCount(1).conversationCount(1)
                            .createDt(now).updateDt(now)
                            .build());
                } catch (DuplicateKeyException e) {
                    incrementUsageStat(agentId, userId, today, now);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update usage stat", e);
        }
    }

    private int incrementUsageStat(Long agentId, Long userId, LocalDate today, LocalDateTime now) {
        return usageStatMapper.update(null, new LambdaUpdateWrapper<AgentUsageStatPO>()
                .eq(AgentUsageStatPO::getAgentId, agentId)
                .eq(AgentUsageStatPO::getUserId, userId)
                .eq(AgentUsageStatPO::getStatDate, today)
                .setSql("message_count = message_count + 1")
                .set(AgentUsageStatPO::getUpdateDt, now));
    }
}
