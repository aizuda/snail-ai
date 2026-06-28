package com.aizuda.snail.ai.agent.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体聊天上下文持有器 - 使用 ThreadLocal 存储聊天会话的完整上下文信息
 * 用于在事件监听器、Advisor 等处获取聊天相关的元数据
 *
 * 包含信息：
 * - 身份信息：agentId, userId, conversationId
 * - 模型信息：modelId, modelName, embeddingModelId
 * - 用户信息：userName, department
 * - 记忆配置：memoryEnabled, memoryTopK, embeddingModelId
 * - 其他配置：skillEnabled, mcpEnabled, webSearchEnabled 等
 */
public class AgentChatContextHolder {
    public static final String KEY = "AgentChatContext";

    private static final ThreadLocal<ChatContext> contextHolder = ThreadLocal.withInitial(ChatContext::new);

    /**
     * 聊天上下文数据类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatContext {
        // 身份信息
        private Long agentId;
        private Long userId;
        private String conversationId;

        // 模型信息
        private Long modelId;
        private String modelKey;
        private Long embeddingModelId;

        // 用户信息
        private String userName;
        private String department;

        // 智能体配置
        private Boolean memoryEnabled;
        private Boolean skillEnabled;
        private Boolean mcpEnabled;
        private Boolean webSearchEnabled;

        // 记忆系统配置
        private Integer memoryTopK;  // 检索Top-K条记忆，默认5
        /** 短期记忆滑动窗口条数（Redis/内存多轮上下文），默认 20 */
        private Integer shortTermMemorySize;
        /** 记忆检索配置 ID（存在时 MemoryRetriever 使用配置中的向量库与召回参数） */
        private Long memoryConfigId;
        private String memoryExtractionType;  // SUMMARY或FULL_TEXT
        private Boolean memorySaveAsync;  // 是否异步保存记忆

        // 其他元数据
        private String agentName;
        private String agentInstruction;

        /** 当前 GENERATION 的思维链内容 */
        private String currentThinkingContent;

    }

    /**
     * 设置完整的聊天上下文
     */
    public static void setContext(ChatContext context) {
        contextHolder.set(context);
    }
    /**
     * 获取完整的聊天上下文
     */
    public static ChatContext getContext() {
        return contextHolder.get();
    }

    /**
     * 清除上下文 - 应在 try-finally 中调用
     */
    public static void clear() {
        contextHolder.remove();
    }


}
