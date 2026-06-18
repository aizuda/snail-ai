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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 短期记忆 JVM 内存存储实现
 *
 * 适用于单机部署或不依赖 Redis 的场景。
 * 使用 ConcurrentHashMap 存储每个会话的有界消息队列（LinkedList），支持滑动窗口。
 *
 * 注意：多实例部署时各节点内存独立，重启后数据丢失（会自动从 DB 回落）。
 *
 * 激活方式：snail.ai.memory.short-term.store-type: memory
 *
 * author: opensnail
 * date: 2026-03-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "snail-ai.memory.short-term.store-type", havingValue = "memory")
public class InMemoryShortTermMemoryStore implements ShortTermMemoryStore {

    private final AgentConversationRecordMapper recordMapper;

    /** conversationId -> 有界消息队列（时间正序） */
    private final ConcurrentHashMap<String, LinkedList<ShortTermMessage>> store = new ConcurrentHashMap<>();

    @Override
    public void append(String conversationId, String role, String content, int windowSize) {
        int cap = Math.max(1, windowSize);
        store.compute(conversationId, (k, list) -> {
            if (list == null) list = new LinkedList<>();
            list.addLast(new ShortTermMessage(role, content));
            // 维护滑动窗口
            while (list.size() > cap) {
                list.removeFirst();
            }
            return list;
        });
    }

    @Override
    public List<ShortTermMessage> loadHistory(ShortTermHistoryQuery query, int windowSize) {
        int cap = Math.max(1, windowSize);
        LinkedList<ShortTermMessage> list = store.get(query.getConversationId());

        List<ShortTermMessage> all;
        if (list == null || list.isEmpty()) {
            // 内存无数据：从 DB 回落并回填
            all = loadFromDbAndFill(query, cap);
        } else {
            all = new ArrayList<>(list);
        }

        // 排除最后一条（当前用户消息，已在 Prompt 中）
        if (all.size() <= 1) {
            return Collections.emptyList();
        }
        return all.subList(0, all.size() - 1);
    }

    @Override
    public void evict(String conversationId) {
        store.remove(conversationId);
    }

    @Override
    public String storeType() {
        return "memory";
    }

    // ==================== 私有方法 ====================

    private List<ShortTermMessage> loadFromDbAndFill(ShortTermHistoryQuery query, int windowSize) {
        List<AgentConversationRecordPO> records = recordMapper.selectList(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, query.getAgentId())
                        .eq(AgentConversationRecordPO::getConversationId, query.getConversationId())
                        .eq(AgentConversationRecordPO::getUserId, query.getUserId())
                        .orderByDesc(AgentConversationRecordPO::getCreateDt)
                        .last("LIMIT " + windowSize));
        Collections.reverse(records);

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<ShortTermMessage> messages = records.stream()
                .map(r -> new ShortTermMessage(r.getRole(), r.getContent()))
                .collect(Collectors.toList());

        // 回填内存
        LinkedList<ShortTermMessage> linked = new LinkedList<>(messages);
        store.put(query.getConversationId(), linked);
        log.debug("短期记忆内存 cache miss，从 DB 回填 {} 条, conversationId={}",
                messages.size(), query.getConversationId());

        return messages;
    }
}
