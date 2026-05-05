package com.aizuda.snail.ai.persistence.memory.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 记忆变更历史持久化对象
 * 表: snail_ai_memory_conversation_history
 *
 * 记录每条记忆的完整变更审计日志
 * 支持版本回溯、审计追踪、变更分析等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_memory_conversation_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemoryHistoryPO {

    /**
     * 历史记录ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 记忆ID (外键)
     * 关联到 snail_ai_conversation_memory.id
     * 该历史记录所属的记忆
     */
    private Long memoryId;

    /**
     * 向量ID (外键)
     * 向量库中对应记忆的唯一标识
     * 用于追踪向量库中的操作
     */
    private String vectorId;

    /**
     * 变更事件类型
     * ADD: 新增记忆
     * UPDATE: 更新记忆
     * DELETE: 删除记忆
     * NOOP: 无操作（评估但未变更）
     */
    private Integer event;

    /**
     * 变更前的记忆内容
     * 变更前记忆的快照（通常为JSON）
     * DELETE事件时为删除前的完整记忆
     * ADD事件时为null
     */
    private String oldMemory;

    /**
     * 变更后的记忆内容
     * 变更后记忆的快照（通常为JSON）
     * ADD/UPDATE事件时为新/更新后的记忆
     * DELETE事件时为null
     */
    private String newMemory;

    /**
     * 操作者用户ID (外键, 可为null)
     * 关联到 snail_ai_user.id
     * 执行该变更的用户（如果是自动操作则为null）
     */
    private Long actorId;

    /**
     * 操作者角色
     * USER: 用户手动操作
     * AGENT: Agent自动操作
     * SYSTEM: 系统自动操作
     */
    private Integer actorRole;

    /**
     * 创建时间
     * 该变更操作的执行时刻
     */
    private LocalDateTime createDt;
}
