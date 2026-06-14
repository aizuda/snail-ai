package com.aizuda.snail.ai.agent.example.controller;

import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.*;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * OpenAPI 使用示例 Controller
 * <p>
 * 演示如何使用 OpenAPI Client 调用 Snail AI 服务端接口
 * 
 * <pre>
 * 访问 Swagger UI: http://localhost:17889/swagger-ui.html
 * </pre>
 *
 * @author opensnail
 * @date 2026-04-25
 */
@RestController
@RequestMapping("/demo")
@ConditionalOnProperty(prefix = "snail-ai.openapi", name = "enabled", havingValue = "true")
@Tag(name = "OpenAPI Demo", description = "OpenAPI 客户端使用示例")
public class OpenApiDemoController {

    private static final Logger log = LoggerFactory.getLogger(OpenApiDemoController.class);

    private final OpenApiAgentClient agentClient;
    private final OpenApiChatClient chatClient;
    private final OpenApiConversationClient conversationClient;
    private final OpenApiUserClient userClient;

    public OpenApiDemoController(OpenApiAgentClient agentClient,
                                 OpenApiChatClient chatClient,
                                 OpenApiConversationClient conversationClient,
                                 OpenApiUserClient userClient) {
        this.agentClient = agentClient;
        this.chatClient = chatClient;
        this.conversationClient = conversationClient;
        this.userClient = userClient;
    }

    // ==================== User 相关接口 ====================

    @PostMapping("/user/register")
    @Operation(summary = "注册用户", description = "注册或更新用户信息，返回 openId")
    public Result<OpenApiUserVO> registerUser(
            @Parameter(description = "用户注册请求")
            @RequestBody OpenApiUserRegisterRequest request) {
        log.info("Register user: externalId={}, nickname={}", request.getExternalId(), request.getNickname());
        return userClient.register(request);
    }

    @GetMapping("/user")
    @Operation(summary = "获取用户信息", description = "根据 openId 查询用户详细信息")
    public Result<OpenApiUserVO> getUser(
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId) {
        OpenApiUserQueryRequest request = new OpenApiUserQueryRequest();
        request.setOpenId(openId);
        return userClient.getUser(request);
    }

    // ==================== Agent 相关接口 ====================

    @GetMapping("/agents")
    @Operation(summary = "获取所有 Agent 列表", description = "查询当前用户可访问的所有智能体")
    public Result<List<OpenApiAgentVO>> listAgents(OpenApiAgentQueryRequest openApiAgentQueryRequest) {
        return agentClient.listAgents(openApiAgentQueryRequest);
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取 Agent 详情", description = "根据 ID 查询智能体详细信息")
    public Result<OpenApiAgentVO> getAgent(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId) {
        OpenApiAgentIdentityRequest request = new OpenApiAgentIdentityRequest();
        request.setAgentId(agentId);
        return agentClient.getAgent(request);
    }

    // ==================== Conversation 相关接口 ====================

    @PostMapping("/agent/{agentId}/conversation")
    @Operation(summary = "创建会话", description = "为指定 Agent 创建一个新的对话会话")
    public Result<OpenApiConversationVO> createConversation(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "创建会话请求") 
            @RequestBody OpenApiCreateConversationRequest request) {
        request.setAgentId(agentId);
        return conversationClient.createConversation(request);
    }

    @GetMapping("/agent/{agentId}/conversations")
    @Operation(summary = "获取会话列表", description = "查询指定 Agent 的所有会话（分页）")
    public PageResult<List<OpenApiConversationVO>> listConversations(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        OpenApiConversationQueryRequest request = new OpenApiConversationQueryRequest();
        request.setAgentId(agentId);
        request.setOpenId(openId);
        request.setPage(page);
        request.setSize(size);
        return conversationClient.listConversations(request);
    }

    @GetMapping("/agent/{agentId}/conversation/{conversationId}/messages")
    @Operation(summary = "获取会话消息", description = "查询指定会话的所有消息记录")
    public Result<List<OpenApiMessageVO>> getMessages(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "会话 ID", required = true, example = "conv-123")
            @PathVariable String conversationId,
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(openId);
        return conversationClient.getMessages(request);
    }

    @DeleteMapping("/agent/{agentId}/conversation/{conversationId}")
    @Operation(summary = "删除会话", description = "删除指定的对话会话")
    public Result<Void> deleteConversation(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "会话 ID", required = true, example = "conv-123")
            @PathVariable String conversationId,
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(openId);
        return conversationClient.deleteConversation(request);
    }

    // ==================== Chat 相关接口 ====================

    @PostMapping("/agent/{agentId}/chat/sync")
    @Operation(summary = "同步对话", description = "发送消息并等待 AI 回复（非流式）")
    public Result<OpenApiChatSyncResponse> chatSync(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "对话请求")
            @RequestBody OpenApiChatRequest request) {
        request.setAgentId(agentId);
        log.info("Sync chat request: agentId={}, content={}", agentId, request.getContent());
        return chatClient.chatSync(request);
    }

    @GetMapping(value = "/agent/{agentId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "发送消息并以 SSE 流式接收 AI 回复")
    public Flux<ServerSentEvent<String>> chatStream(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId,
            @Parameter(description = "用户消息", required = true, example = "你好")
            @RequestParam String content,
            @Parameter(description = "会话 ID（可选）", example = "conv-123")
            @RequestParam(required = false) String conversationId) {

        OpenApiChatRequest request = new OpenApiChatRequest();
        request.setAgentId(agentId);
        request.setOpenId(openId);
        request.setContent(content);
        request.setConversationId(conversationId);

        log.info("Stream chat request: agentId={}, content={}", agentId, content);

        return chatClient.chatStream(request)
                .map(event -> ServerSentEvent.<String>builder(event.getData())
                        .event(event.getType())
                        .build());
    }
}
