package com.aizuda.snail.ai.common.dto.agent;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天分发请求
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDispatchRequest {
    
    /** 请求ID */
    private String requestId;

    /** 提交ID，用于区分同一用户在同一对话中的并发请求 */
    private String sid;

    /** 追踪ID (由 admin 生成，贯穿 agent 侧) */
    private String traceId;

    /** 整个交互的根观测 SPAN ID（服务端创建，客户端 agent_execution 应挂到该节点下） */
    private String rootSpanId;
    
    /** 智能体配置 */
    private AgentConfig agentConfig;
    
    /** 用户信息 */
    private UserInfo userInfo;
    
    /** 会话ID */
    private String conversationId;
    
    /** 用户消息 */
    private String userMessage;
    
    /** 模型配置 */
    private ModelConfig modelConfig;
    
    /** MCP 服务列表 */
    private List<McpServerDescriptor> mcpServers;
    
    /** 技能列表 */
    private List<SkillDescriptor> skills;
    
    /** 历史消息 */
    private List<HistoryMessage> historyMessages;
    
    /** 记忆上下文 */
    private String memoryContext;
    
    /** 系统提示词 */
    private String systemPrompt;

    /**
     * 服务端web端口
     */
    private Integer serverHttpPort;
    
    /**
     * 智能体配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentConfig {
        private Long agentId;
        private String name;
        private String instruction;
        private Boolean mcpEnabled;
        private Boolean skillEnabled;
        private Boolean ragEnabled;
        private Boolean memoryEnabled;
        private Long memoryConfigId;
        private Long embeddingModelId;
        private String ragIds;
        private Integer ragCallMode;
    }
    
    /**
     * 用户信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String userName;
        private String openId;
    }
    
    /**
     * 模型配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelConfig {
        private String modelKey;
        private String apiEndpoint;
        private String apiKey;
        private ConfigExtAttrsDTO configJson;
    }
    
    /**
     * MCP 服务描述符
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpServerDescriptor {
        private Long id;
        private String name;
        private Integer transportType;
        private String baseUri;
        private String endpoint;
        private String command;
        private List<String> args;
        private Map<String, String> envVars;
        private Integer authType;
        private Map<String, String> authConfig;
    }
    
    /**
     * 技能描述符
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDescriptor {
        private Long id;
        private String name;
        private String description;
        private String version;
        /** SKILL.md 等完整技能正文 */
        private String skillPrompt;
    }
    
    /**
     * 历史消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
