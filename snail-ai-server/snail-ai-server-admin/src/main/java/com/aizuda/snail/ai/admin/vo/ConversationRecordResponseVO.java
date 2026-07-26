package com.aizuda.snail.ai.admin.vo;

import lombok.Data;

/**
 * 会话记录响应：单轮问答内容及处理状态。
 *
 * @author opensnail
 * @since 2025-07-13
 */
@Data
public class ConversationRecordResponseVO {

    private Long id;

    public String conversationId;

    private String question;

    private String answer;

    private Integer status;

}
