package com.aizuda.snail.ai.feature.agent.chain;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.ClientInstanceManager;
import com.aizuda.snail.ai.route.ClientRouteStrategy;
import com.aizuda.snail.ai.route.ClientRouteStrategyManager;
import com.aizuda.snail.ai.route.RouteStrategyType;
import com.aizuda.snail.ai.common.enums.CommonStatusEnum;
import com.aizuda.snail.ai.memory.dto.ShortTermHistoryQuery;
import com.aizuda.snail.ai.memory.store.ShortTermMemoryStore;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.app.mapper.AppMapper;
import com.aizuda.snail.ai.persistence.app.po.AppPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 在系统提示与工具数据就绪后，补充短期历史、长期记忆与目标 Client，供 {@link LlmCallHandler} 组装分发请求。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Component
@Order(75)
@RequiredArgsConstructor
public class ContextCollectorHandler implements AgentChatHandler {

    private static final int DEFAULT_SHORT_TERM_WINDOW = 20;

    private final ShortTermMemoryStore shortTermMemoryStore;
    private final ClientInstanceManager instanceManager;
    private final ClientRouteStrategyManager routeStrategyManager;
    private final AppMapper appMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }

        AgentPO agent = ctx.getAgent();
        String appId = agent.getAppId();
        if (appId == null || appId.isBlank()) {
            sendError(ctx, "Agent 未配置 appId，无法进行远程分发");
            return;
        }

        AppPO app = findEnabledApp(appId);
        if (app == null) {
            sendError(ctx, "应用不存在或已停用: " + appId);
            return;
        }

        List<ClientInstanceManager.ClientInstanceInfo> candidates = instanceManager.getAliveInstances(appId);
        if (candidates.isEmpty()) {
            sendError(ctx, "没有可用的客户端实例: " + appId);
            return;
        }

        String routeKey = StrUtil.isNotBlank(app.getRouteStrategy()) ? app.getRouteStrategy() : RouteStrategyType.LEAST_LOAD;
        ClientRouteStrategy routeStrategy = routeStrategyManager.get(routeKey);
        ClientInstanceManager.ClientInstanceInfo target = routeStrategy.select(candidates, ctx.getConversationId());
        ctx.setTargetClient(target);

        int shortTermWindow = agent.getShortTermMemorySize() != null && agent.getShortTermMemorySize() > 0
                ? agent.getShortTermMemorySize()
                : DEFAULT_SHORT_TERM_WINDOW;
        boolean shortTermEnabled = !Boolean.FALSE.equals(agent.getMemoryEnabled());
        if (shortTermEnabled) {
            shortTermMemoryStore.append(ctx.getConversationId(), "user", ctx.getContent(), shortTermWindow);
            ctx.setHistoryMessages(loadHistoryMessages(ctx, shortTermWindow));
        } else {
            ctx.setHistoryMessages(List.of());
        }

        log.info("Context collected for dispatch: appId={}, client={}:{}, historySize={}, shortTermEnabled={}, memoryPresent={}",
                appId, target.getHostIp(), target.getGrpcPort(),
                ctx.getHistoryMessages().size(),
                shortTermEnabled,
                ctx.getMemoryContext() != null && !ctx.getMemoryContext().isEmpty());
    }

    /** 查询启用状态的应用；不存在或已停用返回 null，由调用方终止链路 */
    private AppPO findEnabledApp(String appId) {
        return appMapper.selectOne(
                new LambdaQueryWrapper<AppPO>()
                        .eq(AppPO::getAppId, appId)
                        .eq(AppPO::getStatus, CommonStatusEnum.ENABLED.getValue()));
    }

    /** 加载短期对话历史并转换为分发消息；加载失败降级为空历史，不阻断本次对话 */
    private List<ChatDispatchRequest.HistoryMessage> loadHistoryMessages(AgentChatContext ctx, int windowSize) {
        try {
            ShortTermHistoryQuery query = ShortTermHistoryQuery.builder()
                    .conversationId(ctx.getConversationId())
                    .agentId(ctx.getAgentId())
                    .userId(ctx.getUser().getId())
                    .shortTermMemorySize(windowSize)
                    .build();
            var history = shortTermMemoryStore.loadHistory(query, windowSize);
            if (history == null) {
                return List.of();
            }
            return history.stream()
                    .map(msg -> ChatDispatchRequest.HistoryMessage.builder()
                            .role(msg.getRole())
                            .content(msg.getContent())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("加载短期历史失败，降级为空历史, conversationId={}", ctx.getConversationId(), e);
            return List.of();
        }
    }

    /** 向前端发送错误并终止责任链，同时记录日志便于定位分发失败原因 */
    private void sendError(AgentChatContext ctx, String message) {
        log.warn("上下文收集失败终止链路: {}, conversationId={}, agentId={}",
                message, ctx.getConversationId(), ctx.getAgentId());
        ctx.getStreamWriter().send("[ERROR] " + message);
        ctx.getStreamWriter().complete();
        ctx.setTerminated(true);
    }
}
