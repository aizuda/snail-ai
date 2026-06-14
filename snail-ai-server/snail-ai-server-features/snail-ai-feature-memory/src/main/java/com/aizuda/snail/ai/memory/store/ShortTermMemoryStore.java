package com.aizuda.snail.ai.memory.store;

import com.aizuda.snail.ai.memory.dto.ShortTermHistoryQuery;
import com.aizuda.snail.ai.memory.dto.ShortTermMessage;

import java.util.List;

/**
 * 短期记忆存储策略接口
 *
 * 通过策略模式支持多种存储介质（Redis、JVM 内存等），
 * 通过配置 snail.ai.memory.short-term.store-type 切换实现。
 *
 * author: opensnail
 * date: 2026-03-26
 */
public interface ShortTermMemoryStore {

    /**
     * 追加一条消息到短期记忆（写路径）
     *
     * @param conversationId 会话 ID
     * @param role           消息角色（user / assistant）
     * @param content        消息内容
     * @param windowSize     滑动窗口保留的最大条数（&gt;=1）
     */
    void append(String conversationId, String role, String content, int windowSize);

    /**
     * 加载历史消息（读路径）
     *
     * 返回时间正序列表，不含最后一条（当前用户消息已在 Prompt 中，避免重复）。
     * 如果存储中没有数据（冷启动/重启），实现类应自行回落 DB 并回填。
     *
     * @param query      查询参数（含 agentId、userId、conversationId）
     * @param windowSize 滑动窗口与 DB 回落 LIMIT（&gt;=1）
     * @return 历史消息列表（不含最新一条）
     */
    List<ShortTermMessage> loadHistory(ShortTermHistoryQuery query, int windowSize);

    /**
     * 主动清除指定会话的短期记忆（可在会话结束时调用）
     *
     * @param conversationId 会话 ID
     */
    void evict(String conversationId);

    /**
     * 存储类型标识，与配置项 store-type 对应
     *
     * @return 类型标识，如 "redis"、"memory"
     */
    String storeType();
}
