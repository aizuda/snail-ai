package com.aizuda.snail.ai.memory.store;

import com.aizuda.snail.ai.memory.dto.ShortTermHistoryQuery;
import com.aizuda.snail.ai.memory.dto.ShortTermMessage;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 短期记忆数据库存储实现
 *
 * 适用于分布式部署场景，将短期记忆直接存储到数据库。
 * 所有短期记忆操作都通过 AgentConversationRecord 表持久化。
 *
 * 优点：
 * - 支持多实例部署（共享数据库）
 * - 实例重启后数据不丢失
 * - 便于审计和历史追踪
 *
 * 缺点：
 * - 性能低于 Redis/内存实现（每次操作都是 DB 调用）
 * - 需要定期清理过期数据
 *
 * 激活方式：snail.ai.memory.short-term.store-type: db
 *
 * author: opensnail
 * date: 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "snail-ai.memory.short-term.store-type", matchIfMissing = true, havingValue = "db")
public class DbShortTermMemoryStore implements ShortTermMemoryStore {

    private final AgentConversationRecordMapper recordMapper;

    @Override
    public void append(String conversationId, String role, String content, int windowSize) {
        // 在ConversationRecordCallbackHandler和ConversationHandler中已经写入了
    }

    @Override
    public List<ShortTermMessage> loadHistory(ShortTermHistoryQuery query, int windowSize) {
        int cap = Math.max(1, windowSize);

        // 从 DB 读取最新的 windowSize 条记录（不含最后一条）
        List<AgentConversationRecordPO> records = recordMapper.selectList(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, query.getAgentId())
                        .eq(AgentConversationRecordPO::getConversationId, query.getConversationId())
                        .eq(AgentConversationRecordPO::getUserId, query.getUserId())
                        .orderByAsc(AgentConversationRecordPO::getCreateDt)
                        .last("LIMIT " + cap));

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<ShortTermMessage> messages = records.stream()
                .map(r -> new ShortTermMessage(r.getRole(), r.getContent()))
                .collect(Collectors.toList());

        // 排除最后一条（当前用户消息，已在 Prompt 中）
        if (messages.size() <= 1) {
            return Collections.emptyList();
        }

        return messages.subList(0, messages.size() - 1);
    }

    @Override
    public void evict(String conversationId) {
        // 删除指定会话的所有短期记忆记录
        int deleted = recordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getConversationId, conversationId));

        log.debug("短期记忆 DB 清理: conversationId={}, deletedCount={}", 
                conversationId, deleted);
    }

    @Override
    public String storeType() {
        return "db";
    }
}
