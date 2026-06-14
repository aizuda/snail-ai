package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话记忆 DTO
 *
 * @author snail-ai
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemoryDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对话ID
     */
    private String conversationId;

    /**
     * 来源消息ID
     */
    private Long sourceMessageId;

    /**
     * 记忆类型
     */
    private Integer memoryType;

    /**
     * 类别
     */
    private String category;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 向量库实例ID
     */
    private Long vectorStoreInstanceId;

    /**
     * 向量ID
     */
    private String vectorId;

    /**
     * 向量嵌入（用于插入向量库）
     */
    private List<Float> embedding;

    /**
     * 相关度评分
     */
    private BigDecimal relevanceScore;

    /**
     * 置信度评分
     */
    private BigDecimal confidenceScore;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 最后访问时间
     */
    private LocalDateTime accessedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     */
    private LocalDateTime updateDt;
}
