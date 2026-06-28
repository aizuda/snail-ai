package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.ClientInstanceManager;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.mcp.po.McpServerPO;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.util.List;

/**
 * 智能体对话责任链上下文，在各 Handler 间流转并被逐步填充
 */
@Data
public class AgentChatContext {

    // ── 输入（构造时固定）──
    private final Long agentId;
    private final String conversationId;
    private final String content;
    private final ChatStreamWriter streamWriter;
    /**
     * OpenAPI 场景下显式传入的请求用户（非 OpenAPI 场景为空）
     */
    private final UserPO requestUser;
    /**
     * OpenAPI 场景下显式传入的 openId（非 OpenAPI 场景为空）
     */
    private final String openId;

    /**
     * 提交ID，用于区分同一用户在同一对话中的并发请求
     */
    private String sid;

    /**
     * 用户禁用的 MCP 服务 ID 列表
     */
    private List<Long> disabledMcpServerIds;
    /**
     * 用户禁用的技能 ID 列表
     */
    private List<Long> disabledSkillIds;

    /**
     * 前置 Handler 已失败并结束响应时置为 true，后续 Handler 应短路
     */
    private boolean terminated;

    /**
     * 是否已开始 gRPC 流式分发（用于区分同步失败与异步回调收尾）
     */
    private boolean streamDispatchStarted;

    // ── 由各 Handler 依次填充 ──
    private UserPO user;
    private AgentPO agent;
    private Long modelId;
    private String systemPrompt;
    private List<McpServerPO> mcpServers = Lists.newArrayList();
    private List<ToolCallback> toolCallbacks = Lists.newArrayList();

    // ── 远程分发传输数据（由对应 Handler 填充，LlmCallHandler 统一组装）──

    /**
     * 模型配置（含解密后的 API Key）— ModelResolveHandler
     */
    private ChatDispatchRequest.ModelConfig modelConfig;

    /**
     * MCP 服务描述符（含内置 RAG MCP）— McpHandler
     */
    private List<ChatDispatchRequest.McpServerDescriptor> mcpServerDescriptors = Lists.newArrayList();

    /**
     * Skill 描述符（含 skillContent）— SkillAgentChatHandler
     */
    private List<ChatDispatchRequest.SkillDescriptor> skillDescriptors = Lists.newArrayList();

    /**
     * 历史消息 — ContextCollectorHandler
     */
    private List<ChatDispatchRequest.HistoryMessage> historyMessages = Lists.newArrayList();

    /**
     * 长期记忆上下文 — ContextCollectorHandler
     */
    private String memoryContext;

    /**
     * 目标 Client — ContextCollectorHandler
     */
    private ClientInstanceManager.ClientInstanceInfo targetClient;
}
