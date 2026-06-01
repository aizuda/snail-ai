package com.aizuda.snail.ai.agent.core.grpc.handler;

import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;
import com.aizuda.snail.ai.agent.core.resolver.*;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.common.constants.SystemConstants;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcStreamingRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户端 Chat 分发（流式）：{@link UriConstants#CHAT_DISPATCH}
 */
@Slf4j
@RequiredArgsConstructor
public class ChatDispatchStreamingHandler implements GrpcStreamingRequestHandler {

    private static final int REQUEST_STATUS_SUCCESS = 1;
    private static final int REQUEST_STATUS_FAILED = 0;
    private static final String ERROR_CODE_LLM_FAILED = "LLM_FAILED";

    private final ClientChatExecutor chatExecutor;
    private final ActiveChatCounter activeChatCounter;
    private final ClientSkillToolResolver skillToolResolver;
    private final ClientRagToolResolver ragToolResolver;
    private final ClientMemoryToolResolver memoryToolResolver;
    private final CustomToolCallbackProvider customToolCallbackProvider;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CHAT_DISPATCH.equals(uri);
    }

    @Override
    public void handle(GrpcHandlerRequest request,
                       StreamObserver<GrpcSnailAiResult> observer) {
        activeChatCounter.increment();
        long reqId = request.getReqId();
        ChatDispatchRequest dispatchRequest = parseDispatchRequest(request.getBody());
        String requestId = dispatchRequest.getRequestId();

        log.info("Chat dispatch received: requestId={}", requestId);

        ClientMcpToolResolver mcpResolver = new ClientMcpToolResolver();
        List<ToolCallback> tools = resolveAllTools(dispatchRequest, mcpResolver);

        chatExecutor.executeStream(
                dispatchRequest,
                tools,
                text -> handleTextChunk(reqId, text, observer),
                thinking -> handleThinkingChunk(reqId, thinking, observer),
                completion -> handleCompletion(reqId, requestId, completion, observer, mcpResolver),
                error -> handleError(reqId, error, observer, mcpResolver));
    }

    private ChatDispatchRequest parseDispatchRequest(String body) {
        return JsonUtil.parseObject(body, ChatDispatchRequest.class);
    }

    private List<ToolCallback> resolveAllTools(ChatDispatchRequest dispatchRequest,
                                               ClientMcpToolResolver mcpResolver) {

        // 过滤掉 RAG MCP 描述符（已改为本地 Tool）
        filterRagMcpDescriptor(dispatchRequest);

        // MCP 工具
        List<ToolCallback> tools = new ArrayList<>(resolveMcpTools(dispatchRequest.getMcpServers(), mcpResolver));

        // RAG 知识库搜索工具（本地 Tool，通过 gRPC 回调服务端）
        try {
            tools.addAll(ragToolResolver.resolve(dispatchRequest));
        } catch (Exception e) {
            log.warn("Failed to resolve RAG tools", e);
        }

        // Skill 工具（read_skill + shell + http_request）
        try {
            tools.addAll(skillToolResolver.resolve(dispatchRequest));
        } catch (Exception e) {
            log.warn("Failed to resolve Skill tools", e);
        }

        // 用户自定义 @Tool Bean（启动时已缓存）
        tools.addAll(Arrays.asList(customToolCallbackProvider.getToolCallbacks()));

        return tools;
    }

    private void filterRagMcpDescriptor(ChatDispatchRequest dispatchRequest) {
        List<ChatDispatchRequest.McpServerDescriptor> mcpServers = dispatchRequest.getMcpServers();
        if (mcpServers != null && !mcpServers.isEmpty()) {
            mcpServers.removeIf(s -> SystemConstants.RAG_MCP_SERVER_NAME.equals(s.getName()));
        }
    }

    private List<ToolCallback> resolveMcpTools(List<ChatDispatchRequest.McpServerDescriptor> mcpServers,
                                               ClientMcpToolResolver mcpResolver) {
        if (mcpServers == null || mcpServers.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(mcpResolver.resolve(mcpServers));
        } catch (Exception e) {
            log.warn("Failed to resolve MCP tools", e);
            return new ArrayList<>();
        }
    }

    private void handleTextChunk(long reqId, String text, StreamObserver<GrpcSnailAiResult> observer) {
        try {
            ChatStreamResponse response = ChatStreamResponse.text(text);
            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send chunk", e);
        }
    }

    private void handleThinkingChunk(long reqId, String text, StreamObserver<GrpcSnailAiResult> observer) {
        try {
            ChatStreamResponse response = ChatStreamResponse.thinking(text);
            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send thinking chunk", e);
        }
    }

    private void handleCompletion(long reqId, String requestId,
                                  ClientChatExecutor.ChatCompletionResult completion,
                                  StreamObserver<GrpcSnailAiResult> observer,
                                  ClientMcpToolResolver mcpResolver) {
        try {
            ChatStreamResponse response = ChatStreamResponse.completion(
                    completion.fullText(),
                    completion.fullThinking(),
                    completion.promptTokens(),
                    completion.completionTokens(),
                    completion.durationMs());

            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
            observer.onCompleted();
            log.info("Chat completed: requestId={}, duration={}ms", requestId, completion.durationMs());
        } finally {
            cleanupResources(mcpResolver);
        }
    }

    private void handleError(long reqId, Throwable error,
                             StreamObserver<GrpcSnailAiResult> observer,
                             ClientMcpToolResolver mcpResolver) {
        try {
            String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";
            ChatStreamResponse response = ChatStreamResponse.error(ERROR_CODE_LLM_FAILED, errorMessage);

            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_FAILED)
                    .setData(data)
                    .build());
            observer.onCompleted();
        } finally {
            cleanupResources(mcpResolver);
        }
    }

    private void cleanupResources(ClientMcpToolResolver mcpResolver) {
        activeChatCounter.decrement();
        try {
            mcpResolver.close();
        } catch (Exception e) {
            log.warn("Failed to close MCP tool resolver", e);
        }
    }
}
