package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体对话记录视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationRecordVO {

    /** 消息角色（user/assistant/system） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 模型思考过程 */
    private String thinking;

    private String metadata;

    /** 消息状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createDt;
}
