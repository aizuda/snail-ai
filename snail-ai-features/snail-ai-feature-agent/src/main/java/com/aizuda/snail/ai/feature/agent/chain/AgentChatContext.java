package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.ClientInstanceManager;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.mcp.po.McpServerPO;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能体对话责任链上下文，在各 Handler 间流转并被逐步填充
 */
@Data
public class AgentChatContext {

    // ── 输入（构造时固定）──
    private final Long agentId;
    private final String conversationId;
    private final String content;
    private final ResponseBodyEmitter emitter;
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
     * 追踪ID，由 AgentChatService 生成，贯穿整个对话链路
     */
    private String traceId;

    /**
     * 整个交互的根观测 SPAN ID（覆盖从 HTTP 请求到响应完成），由 AgentChatService 生成
     */
    private String rootSpanId;

    /**
     * root SPAN 开始时间（epoch millis），由 AgentChatService 生成
     */
    private Long rootSpanStartTimeMs;

    /**
     * 是否已开始 gRPC 流式分发（用于区分同步失败与异步回调收尾）
     */
    private boolean streamDispatchStarted;

    /**
     * root SPAN 是否已收尾，避免并发重复写入
     */
    private final AtomicBoolean rootSpanClosed = new AtomicBoolean(false);

    /**
     * Context Preparation SPAN ID，由 InitContextHandler 生成，用于关联配置下发子观测
     */
    private String contextPreparationSpanId;

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
