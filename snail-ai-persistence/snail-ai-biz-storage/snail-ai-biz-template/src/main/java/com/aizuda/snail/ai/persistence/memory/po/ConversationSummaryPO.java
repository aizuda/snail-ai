package com.aizuda.snail.ai.persistence.memory.po;

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
 * 对话摘要PO
 */
@TableName("sai_memory_conversation_summary")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationSummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;
    private Long agentId;
    private Long userId;
    private Integer summaryType;  // 1-INCREMENTAL 2-FULL
    private String messageRange;  // JSON
    private String summaryText;
    private String keyPoints;  // JSON数组
    private Integer tokenCount;
    private BigDecimal compressedRatio;
    private LocalDateTime createDt;
    private LocalDateTime expiresAt;
}
