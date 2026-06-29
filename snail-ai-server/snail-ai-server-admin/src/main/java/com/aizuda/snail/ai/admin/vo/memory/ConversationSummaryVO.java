package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话摘要视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummaryVO {

    /** 会话 ID */
    private String conversationId;
    /** 会话标题 */
    private String title;
    /** 用户名 */
    private String userName;
    /** 消息数量 */
    private int messageCount;
    /** 工具调用次数 */
    private int toolCallCount;
    /** 创建时间 */
    private String createDt;
    /** 最后一条消息时间 */
    private String lastMessageDt;
}
