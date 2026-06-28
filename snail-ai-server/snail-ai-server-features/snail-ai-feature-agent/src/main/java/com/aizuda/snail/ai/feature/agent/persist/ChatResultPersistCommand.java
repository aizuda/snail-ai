package com.aizuda.snail.ai.feature.agent.persist;

import lombok.Builder;
import lombok.Data;

/**
 * Chat 流式响应结束后的持久化参数：承载助手消息、思维链、Token 用量与短期记忆配置，
 * 供 {@link ChatResultPersistService} 异步落库使用。
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
