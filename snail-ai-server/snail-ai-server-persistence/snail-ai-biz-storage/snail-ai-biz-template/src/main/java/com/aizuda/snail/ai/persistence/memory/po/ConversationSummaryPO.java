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
 * 会话摘要持久化对象
 */
@TableName("sai_conversation_summary")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationSummaryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Long userId;

    private String conversationId;

    private String summary;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
