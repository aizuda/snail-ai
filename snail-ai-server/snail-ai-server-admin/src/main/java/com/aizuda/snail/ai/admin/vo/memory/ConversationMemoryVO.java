package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话记忆VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemoryVO {

    private Long id;
    private Long agentId;
    private Long userId;
    private String conversationId;
    private Long sourceMessageId;

    private Integer memoryType;
    private String category;
    private String title;
    private String content;
    private List<String> tags;

    private BigDecimal relevanceScore;
    private BigDecimal confidenceScore;
    private Integer status;
    private LocalDateTime accessedAt;
    private Integer accessCount;

    private LocalDateTime createDt;
    private LocalDateTime updateDt;
}
