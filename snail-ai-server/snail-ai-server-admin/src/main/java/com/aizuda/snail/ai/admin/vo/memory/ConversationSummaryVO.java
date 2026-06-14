package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummaryVO {

    private String conversationId;
    private String title;
    private String userName;
    private int messageCount;
    private int toolCallCount;
    private String createDt;
    private String lastMessageDt;
}
