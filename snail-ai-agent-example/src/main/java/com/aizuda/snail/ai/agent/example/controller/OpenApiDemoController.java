package com.aizuda.snail.ai.agent.example.controller;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.openapi.client.core.listener.SseEventListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
@Slf4j
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
@Tag(name = "OpenAPI Demo", description = "OpenAPI 客户端使用示例")
public class OpenApiDemoController {

    private final OpenApiAgentClient agentClient;
    private final OpenApiChatClient chatClient;
    private final OpenApiConversationClient conversationClient;
    private final OpenApiUserClient userClient;

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
    public Result<List<OpenApiAgentVO>> listAgents() {
        return agentClient.listAgents();
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

    @GetMapping("/agent/{agentId}/chat/stream")
    @Operation(summary = "流式对话", description = "发送消息并以 SSE 流式接收 AI 回复")
    public SseEmitter chatStream(
            @Parameter(description = "Agent ID", required = true, example = "1")
            @PathVariable Long agentId,
            @Parameter(description = "openId", required = true, example = "demo-open-id")
            @RequestParam String openId,
            @Parameter(description = "用户消息", required = true, example = "你好")
            @RequestParam String content,
            @Parameter(description = "会话 ID（可选）", example = "conv-123")
            @RequestParam(required = false) String conversationId) {

        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时

        OpenApiChatRequest request = new OpenApiChatRequest();
        request.setAgentId(agentId);
        request.setOpenId(openId);
        request.setContent(content);
        request.setConversationId(conversationId);

        log.info("Stream chat request: agentId={}, content={}", agentId, content);

        // 异步执行流式对话
        CompletableFuture.runAsync(() -> {
            try {
                chatClient.chatStream(request, new SseEventListener() {
                    @Override
                    public void onText(String text) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("text")
                                    .data(text));
                        } catch (IOException e) {
                            log.error("Failed to send SSE text", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onThinking(String thinking) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("thinking")
                                    .data(thinking));
                        } catch (IOException e) {
                            log.error("Failed to send SSE thinking", e);
                        }
                    }

                    @Override
                    public void onComplete(String data) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(data));
                            emitter.complete();
                            log.info("Stream chat completed");
                        } catch (IOException e) {
                            log.error("Failed to send SSE completion", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        log.error("Stream chat error: {}", errorMessage);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(errorMessage));
                        } catch (IOException e) {
                            log.error("Failed to send SSE error", e);
                        }
                        emitter.completeWithError(new SnailAiException(errorMessage));
                    }
                });
            } catch (Exception e) {
                log.error("Stream chat exception", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
