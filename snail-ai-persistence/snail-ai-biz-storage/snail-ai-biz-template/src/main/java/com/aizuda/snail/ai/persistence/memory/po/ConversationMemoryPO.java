package com.aizuda.snail.ai.persistence.memory.po;

import com.aizuda.snail.ai.common.enums.memory.MemoryStatusEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对话记忆持久化对象
 * 表: snail_ai_memory_conversation
 *
 * 存储从多轮对话中LLM提取出来的结构化记忆
 * 包括事实、决策、偏好、任务进度等
 * 支持向量化存储和语义检索
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_memory_conversation")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemoryPO {

    /**
     * 记忆ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体ID (外键)
     * 关联到 snail_ai_agent.id
     * 该记忆属于的Agent
     */
    private Long agentId;

    /**
     * 用户ID (外键)
     * 关联到 snail_ai_user.id
     * 该记忆涉及的用户
     */
    private Long userId;

    /**
     * 创建者ID (外键, 可为null)
     * 关联到 snail_ai_user.id
     * 创建或更新该记忆的操作者
     */
    private Long actorId;

    /**
     * 创建者角色
     * USER: 用户创建/更新
     * AGENT: Agent创建/更新
     * SYSTEM: 系统自动创建/更新
     */
    private Integer actorRole;

    /**
     * 会话ID (外键)
     * 关联到 snail_ai_agent_conversation.id
     * 该记忆来源的会话
     */
    private String conversationId;

    /**
     * 源消息ID (外键, 可为null)
     * 关联到 snail_ai_agent_conversation_record.id
     * 该记忆提取自的原始消息
     */
    private Long sourceMessageId;

    /**
     * 记忆类型
     * FACT: 事实信息（用户身份、背景等）
     * DECISION: 决策/选择
     * PREFERENCE: 偏好/喜好
     * TASK_PROGRESS: 任务进度
     * REFERENCE: 参考信息
     */
    private Integer memoryType;

    /**
     * 记忆分类
     * 二级分类，用于更细粒度的分组
     * 例如: PERSONAL, WORK, LEARNING等
     */
    private String category;

    /**
     * 记忆标题
     * 记忆的简短标识，便于显示和理解
     */
    private String title;

    /**
     * 记忆内容
     * 结构化的记忆文本，包含具体信息
     */
    private String content;

    /**
     * 内容哈希值 (SHA-256)
     * 用于快速去重和完整性校验
     * 相同内容的不同记忆实例可通过此字段识别
     */
    private String memoryHash;

    /**
     * 记忆标签 (JSON数组格式)
     * 例如: ["重要", "客户", "2026年"]
     * 支持多标签标记和分类
     */
    private String tags;

    /**
     * 向量库实例ID (外键)
     * 关联到 snail_ai_store_instance.id
     * 该记忆的向量存储位置
     */
    private Long vectorStoreInstanceId;

    /**
     * 向量ID (外键)
     * 向量库中该记忆对应向量的唯一标识
     * 用于向量检索和更新
     */
    private String vectorId;

    /**
     * 相关性评分
     * 取值范围: 0.0-1.0
     * 表示该记忆与当前查询/上下文的相关程度
     */
    private BigDecimal relevanceScore;

    /**
     * 置信度评分
     * 取值范围: 0.0-1.0
     * 表示该记忆的准确性和可信度
     * 由LLM在提取时评估
     */
    private BigDecimal confidenceScore;

    /**
     * 记忆状态
     * ACTIVE: 有效/活跃
     * ARCHIVED: 归档/历史
     * DELETED: 删除/禁用
     * 使用MemoryStatusEnum枚举管理
     */
    private MemoryStatusEnum status;

    /**
     * 最后访问时间
     * 记录该记忆最后一次被检索/使用的时刻
     * 用于访问热度计算
     */
    private LocalDateTime accessedAt;

    /**
     * 访问计数
     * 该记忆被检索/使用的累计次数
     * 用于热度排序和重要性评估
     */
    private Integer accessCount;

    /**
     * 创建时间
     * 记忆首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 记忆最后一次修改的时刻
     */
    private LocalDateTime updateDt;

    /**
     * 过期时间
     * null: 记忆永不过期
     * 具体值: 记忆的失效时刻，超过此时间的记忆将被清理
     * 由 MemoryPO.memoryExpirationDays 在写入时计算
     */
    private LocalDateTime expiresAt;
}
