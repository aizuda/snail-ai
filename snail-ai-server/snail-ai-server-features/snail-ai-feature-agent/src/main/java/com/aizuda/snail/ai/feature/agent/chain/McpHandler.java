package com.aizuda.snail.ai.feature.agent.chain;


import com.aizuda.snail.ai.common.mcp.McpServerRef;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP 加载：若智能体启用了 MCP，加载绑定的 MCP 服务器列表，并准备远程分发用的 {@link ChatDispatchRequest.McpServerDescriptor}
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class McpHandler implements AgentChatHandler {

    private final McpToolService mcpToolService;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        List<ChatDispatchRequest.McpServerDescriptor> descriptors = new ArrayList<>();

        // 加载数据库中配置的 MCP 服务器
        if (Boolean.TRUE.equals(ctx.getAgent().getMcpEnabled())) {
            var servers = mcpToolService.getMcpServersForAgent(ctx.getAgentId());
            List<Long> disabledIds = ctx.getDisabledMcpServerIds();
            if (disabledIds != null && !disabledIds.isEmpty()) {
                servers = servers.stream()
                        .filter(s -> !disabledIds.contains(s.getId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            ctx.setMcpServers(servers);
            for (McpServerRef s : servers) {
                descriptors.add(toDescriptor(s));
            }
        }

        ctx.setMcpServerDescriptors(descriptors);
        log.debug("MCP 加载完成 启用={} 服务器 {} 个 耗时 {}ms agentId={}",
                ctx.getAgent().getMcpEnabled(), descriptors.size(),
                System.currentTimeMillis() - startTime, ctx.getAgentId());
    }

    /** 将 MCP 服务器引用转换为远程分发描述符（args/envVars/headers 由 JSON 字符串反序列化） */
    private static ChatDispatchRequest.McpServerDescriptor toDescriptor(McpServerRef s) {
        return ChatDispatchRequest.McpServerDescriptor.builder()
                .id(s.getId())
                .name(s.getName())
                .transportType(s.getTransportType())
                .baseUri(s.getBaseUri())
                .endpoint(s.getEndpoint())
                .command(s.getCommand())
                .args(parseJsonList(s.getArgs()))
                .envVars(parseJsonMap(s.getEnvVars()))
                .timeoutMs(s.getTimeoutMs())
                .headers(parseJsonMap(s.getHeaders()))
                .build();
    }

    /** 解析 JSON 数组配置；解析失败降级为空列表并告警，避免脏配置阻断整个对话链路 */
    @SuppressWarnings("unchecked")
    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JsonUtil.parseObject(json, List.class);
        } catch (Exception e) {
            log.warn("MCP 配置 JSON 数组解析失败，忽略该配置项: {}", json, e);
            return List.of();
        }
    }

    /** 解析 JSON 对象配置；解析失败降级为空 Map 并告警，避免脏配置阻断整个对话链路 */
    private static Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JsonUtil.parseHashMap(json);
        } catch (Exception e) {
            log.warn("MCP 配置 JSON 对象解析失败，忽略该配置项: {}", json, e);
            return Map.of();
        }
    }


}
