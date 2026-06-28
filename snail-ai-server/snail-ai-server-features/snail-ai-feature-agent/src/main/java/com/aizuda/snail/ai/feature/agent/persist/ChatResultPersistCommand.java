package com.aizuda.snail.ai.feature.agent.persist;

import lombok.Builder;
import lombok.Data;

/**
 * ChatResultPersistCommand
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
public class ChatResultPersistCommand {

    private Long agentId;
    private Long userId;
    private String conversationId;
    private String fullText;
    private String thinkingText;
    private int inputTokens;
    private int outputTokens;
    private int cacheTokens;
    private Boolean memoryEnabled;
    private Long memoryConfigId;
    private Long agentModelId;
    private int shortTermWindow;
}
