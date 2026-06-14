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
 * 对话长期记忆持久化对象
 */
@TableName("sai_memory_conversation")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Long userId;

    private String conversationId;

    private Long sourceMessageId;

    private Integer memoryType;

    private String category;

    private String title;

    private String content;

    private String tags;

    private Long vectorStoreInstanceId;

    private String vectorId;

    private BigDecimal relevanceScore;

    private BigDecimal confidenceScore;

    private MemoryStatusEnum status;

    private LocalDateTime expiresAt;

    private LocalDateTime accessedAt;

    private Integer accessCount;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
