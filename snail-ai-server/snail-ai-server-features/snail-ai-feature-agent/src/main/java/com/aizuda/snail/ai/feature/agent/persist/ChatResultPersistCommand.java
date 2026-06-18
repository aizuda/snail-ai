package com.aizuda.snail.ai.feature.agent.persist;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResultPersistCommand {

    private Long agentId;
    private Long userId;
    private String conversationId;
    private String fullText;
    private String thinkingText;
    private Boolean memoryEnabled;
    private Long memoryConfigId;
    private Long agentModelId;
    private int shortTermWindow;
}
