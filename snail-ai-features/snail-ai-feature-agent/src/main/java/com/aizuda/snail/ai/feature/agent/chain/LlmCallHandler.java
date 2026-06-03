package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.feature.agent.persist.ChatResultPersistService;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.grpc.client.GrpcChannelUtil;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * 责任链终点：从 {@link AgentChatContext} 组装 {@link com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest}，通过 gRPC 流式调用远程 Client。
 */
@Slf4j
@Component
@Order(80)
@RequiredArgsConstructor
public class LlmCallHandler implements AgentChatHandler {

    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    private final ChatResultPersistService chatResultPersistService;

    @Value("${server.port:8080}")
    private int serverHttpPort;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }

        if (ctx.getTargetClient() == null) {
            sendError(ctx.getEmitter(), "未找到可用的客户端实例");
            ctx.setTerminated(true);
            return;
        }

        if (ctx.getModelConfig() == null) {
            sendError(ctx.getEmitter(), "模型配置缺失");
            ctx.setTerminated(true);
            return;
        }

        com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest request = buildDispatchRequest(ctx);
        String dispatchBody = JsonUtil.toJsonString(request);

        int shortTermWindow = ctx.getAgent().getShortTermMemorySize() != null && ctx.getAgent().getShortTermMemorySize() > 0
                ? ctx.getAgent().getShortTermMemorySize()
                : DEFAULT_SHORT_TERM_WINDOW;

        log.info("Dispatching chat via chain: host={}:{}, conversationId={}, modelName={}",
                ctx.getTargetClient().getHostIp(),
                ctx.getTargetClient().getGrpcPort(),
                ctx.getConversationId(),
                ctx.getModelConfig().getModelKey());

        try {
            GrpcChannelUtil.sendServerStreaming(
                    ctx.getTargetClient().getChannel(),
                    UriConstants.CHAT_DISPATCH,
                    dispatchBody,
                    Map.of(),
                    new ChatStreamObserver(ctx, chatResultPersistService, shortTermWindow));
            ctx.setStreamDispatchStarted(true);
        } catch (Exception e) {
            ctx.setStreamDispatchStarted(false);
            throw e;
        }
    }

    private ChatDispatchRequest buildDispatchRequest(AgentChatContext ctx) {
        AgentPO agent = ctx.getAgent();
        UserPO user = ctx.getUser();
        String traceId = ensureTraceId(ctx);
        String rootSpanId = ensureRootSpanId(ctx);

        ChatDispatchRequest.AgentConfig agentConfig = ChatDispatchRequest.AgentConfig.builder()
                .agentId(agent.getId())
                .name(agent.getName())
                .instruction(agent.getInstruction())
                .mcpEnabled(agent.getMcpEnabled())
                .skillEnabled(agent.getSkillEnabled())
                .ragEnabled(agent.getRagEnabled())
                .memoryEnabled(agent.getMemoryEnabled())
                .embeddingModelId(ctx.getModelId())
                .ragIds(agent.getRagIds())
                .ragCallMode(agent.getRagCallMode())
                .build();

        ChatDispatchRequest.UserInfo userInfo = ChatDispatchRequest.UserInfo.builder()
                .userId(user.getId())
                .userName(user.getUsername() != null ? user.getUsername() : "")
                .openId(ctx.getOpenId())
                .build();

        return ChatDispatchRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .sid(ctx.getSid())
                .traceId(traceId)
                .rootSpanId(rootSpanId)
                .agentConfig(agentConfig)
                .userInfo(userInfo)
                .conversationId(ctx.getConversationId())
                .userMessage(ctx.getContent())
                .modelConfig(ctx.getModelConfig())
                .mcpServers(ctx.getMcpServerDescriptors())
                .skills(ctx.getSkillDescriptors())
                .historyMessages(ctx.getHistoryMessages())
                .memoryContext(ctx.getMemoryContext())
                .systemPrompt(ctx.getSystemPrompt())
                .serverHttpPort(serverHttpPort)
                .build();
    }

    private String ensureTraceId(AgentChatContext ctx) {
        String traceId = ctx.getTraceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            ctx.setTraceId(traceId);
            log.warn("TraceId missing before dispatch, generated fallback traceId={}", traceId);
        }
        return traceId;
    }

    private String ensureRootSpanId(AgentChatContext ctx) {
        String rootSpanId = ctx.getRootSpanId();
        if (rootSpanId == null || rootSpanId.isBlank()) {
            rootSpanId = UUID.randomUUID().toString();
            ctx.setRootSpanId(rootSpanId);
            if (ctx.getRootSpanStartTimeMs() == null) {
                ctx.setRootSpanStartTimeMs(System.currentTimeMillis());
            }
            log.warn("RootSpanId missing before dispatch, generated fallback rootSpanId={}", rootSpanId);
        }
        return rootSpanId;
    }

    private static void sendError(ResponseBodyEmitter emitter, String message) {
        try {
            emitter.send("[ERROR] " + message);
            emitter.complete();
        } catch (Exception e) {
            log.warn("Failed to send error to emitter", e);
        }
    }
}
