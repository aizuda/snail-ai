package com.aizuda.snail.ai.admin.vo;

import lombok.Data;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2025-07-13
 */
@Data
public class ConversationRecordResponseVO {

    private Long id;

    public String conversationId;

    private String question;

    private String answer;

    private Integer status;

}
