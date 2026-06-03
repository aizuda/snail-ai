package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.AgentChatService;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.openapi.emitter.CollectingEmitter;
import com.aizuda.snail.ai.openapi.emitter.SseWrappingEmitter;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * OpenAPI 对话服务（已简化）
 * AgentChatService 已被移除，此服务保留接口签名以保证兼容性
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiChatService {

    private static final long SYNC_CHAT_TIMEOUT_MS = 300_000L;
    private final OpenApiUserResolver openApiUserResolver;
    private final AgentChatService agentChatService;

    /**
     * 流式对话 — 返回 SseEmitter，通过 SSE 事件推送每个 text chunk
     */
    public SseEmitter chatStream(OpenApiChatRequest request) {
        SseEmitter sseEmitter = new SseEmitter(request.getTimeout());
        SseWrappingEmitter wrapper = new SseWrappingEmitter(sseEmitter, request.getConversationId());
        UserPO requestUser = resolveRequestUser(request.getOpenId());

        String sid = StringUtils.hasText(request.getSid())
                ? request.getSid()
                : UUID.randomUUID().toString();

        agentChatService.chat(AgentChatCommand.builder()
                .agentId(request.getAgentId())
                .conversationId(request.getConversationId())
                .content(request.getContent())
                .disabledMcpServerIds(request.getDisabledMcpServerIds())
                .disabledSkillIds(request.getDisabledSkillIds())
                .emitter(wrapper)
                .requestUser(requestUser)
                .openId(request.getOpenId())
                .sid(sid)
                .build());

        return sseEmitter;
    }

    /**
     * 同步对话 — 阻塞等待完整响应
     */
    public OpenApiChatSyncResponse chatSync(OpenApiChatRequest request) {
        long start = System.currentTimeMillis();
        CollectingEmitter collector = new CollectingEmitter();
        UserPO requestUser = resolveRequestUser(request.getOpenId());

        String sid = StringUtils.hasText(request.getSid())
                ? request.getSid()
                : UUID.randomUUID().toString();

        agentChatService.chat(AgentChatCommand.builder()
                .agentId(request.getAgentId())
                .conversationId(request.getConversationId())
                .content(request.getContent())
                .disabledMcpServerIds(request.getDisabledMcpServerIds())
                .disabledSkillIds(request.getDisabledSkillIds())
                .emitter(collector)
                .requestUser(requestUser)
                .openId(request.getOpenId())
                .sid(sid)
                .build());

        try {
            String fullText = collector.awaitAndGetFullText(SYNC_CHAT_TIMEOUT_MS);
            return OpenApiChatSyncResponse.builder()
                    .conversationId(request.getConversationId())
                    .content(fullText)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SnailAiException("对话被中断", e);
        } catch (TimeoutException e) {
            throw new SnailAiException("对话响应超时", e);
        }
    }

    private UserPO resolveRequestUser(String openId) {
        String appId = OpenApiSessionUtils.current().getAppId();
        return openApiUserResolver.resolvePlatformUser(appId, openId);
    }
}
