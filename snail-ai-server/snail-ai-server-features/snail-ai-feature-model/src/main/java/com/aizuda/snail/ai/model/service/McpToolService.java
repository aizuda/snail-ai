package com.aizuda.snail.ai.model.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.enums.mcp.McpTransportTypeEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMcpServerMapper;
import com.aizuda.snail.ai.persistence.mcp.mapper.McpServerMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentMcpServerPO;
import com.aizuda.snail.ai.common.mcp.McpServerRef;
import com.aizuda.snail.ai.persistence.mcp.po.McpServerPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP 工具服务：动态连接 MCP 服务器，发现工具，转为 Spring AI ToolCallback
 * 支持三种传输方式：SSE、Streamable HTTP、Stdio
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private static final String SSE_TRANSPORT_DEPRECATED_MESSAGE =
            "MCP SSE transport is deprecated in MCP SDK 2.x, please use Streamable HTTP for MCP server: ";

    private final McpServerMapper mcpServerMapper;
    private final AgentMcpServerMapper agentMcpServerMapper;

    /**
     * 缓存已连接的 MCP Client（key=serverId）
     */
    private final Map<Long, McpSyncClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 获取智能体关联的 MCP 服务器列表
     */
    public List<McpServerPO> getMcpServersForAgent(Long agentId) {
        List<AgentMcpServerPO> relations = agentMcpServerMapper.selectList(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getAgentId, agentId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> serverIds = relations.stream()
                .map(AgentMcpServerPO::getMcpServerId)
                .collect(Collectors.toList());
        return mcpServerMapper.selectByIds(serverIds);
    }

    /**
     * 根据 MCP 服务器列表获取所有可用的 ToolCallback。
     */
    public List<ToolCallback> getToolCallbacks(List<? extends McpServerRef> mcpServers) {
        List<McpSyncClient> clients = new ArrayList<>();

        for (McpServerRef server : mcpServers) {
            try {
                McpSyncClient client = getOrCreateClient(server);
                clients.add(client);
            } catch (Exception e) {
                log.warn("Failed to connect MCP server: {} (id={}), error: {}",
                        server.getName(), server.getId(), e.getMessage());
            }
        }

        if (clients.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                    .mcpClients(clients)
                    .build();
            ToolCallback[] callbacks = provider.getToolCallbacks();
            return Arrays.asList(callbacks);
        } catch (Exception e) {
            log.error("Failed to get tool callbacks from MCP servers", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取或创建 MCP Client（带缓存），根据传输类型选择不同的连接方式。
     */
    private McpSyncClient getOrCreateClient(McpServerRef server) {
        return clientCache.computeIfAbsent(server.getId(), id -> {
            Integer transportType = server.getTransportType();
            if (transportType == null) {
                transportType = McpTransportTypeEnum.STREAMABLE_HTTP.getValue();
            }
            McpTransportTypeEnum transportEnum = McpTransportTypeEnum.fromValue(transportType);
            if (transportEnum == null) {
                throw new SnailAiException("Unsupported transport type: " + transportType);
            }

            return switch (transportEnum) {
                case STDIO -> createStdioClient(server);
                case STREAMABLE_HTTP -> createStreamableHttpClient(server);
                case SSE -> throwDeprecatedSseTransport(server.getName());
            };
        });
    }

    /**
     * 创建 Stdio 传输的 MCP Client
     */
    private McpSyncClient createStdioClient(McpServerRef server) {
        String command = server.getCommand();
        if (StrUtil.isBlank(command)) {
            throw new SnailAiException("Stdio transport requires a command for MCP server: " + server.getName());
        }

        ServerParameters.Builder paramsBuilder = ServerParameters.builder(command);

        // 解析参数列表
        if (StrUtil.isNotBlank(server.getArgs())) {
            List<String> args = JsonUtil.parseObject(server.getArgs(), new TypeReference<List<String>>() {});
            if (args != null && !args.isEmpty()) {
                paramsBuilder.args(args);
            }
        }

        // 解析环境变量
        if (StrUtil.isNotBlank(server.getEnvVars())) {
            Map<String, String> envVars = JsonUtil.parseObject(server.getEnvVars(), new TypeReference<Map<String, String>>() {});
            if (envVars != null && !envVars.isEmpty()) {
                paramsBuilder.env(envVars);
            }
        }

        ServerParameters params = paramsBuilder.build();
        JacksonMcpJsonMapperSupplier supplier = new JacksonMcpJsonMapperSupplier();
        McpJsonMapper jsonMapper = supplier.get();
        StdioClientTransport transport = new StdioClientTransport(params, jsonMapper);
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        log.info("MCP client connected via Stdio to: {} (command: {})", server.getName(), command);
        return client;
    }

    /**
     * 创建 Streamable HTTP 传输的 MCP Client
     */
    private McpSyncClient createStreamableHttpClient(McpServerRef server) {
        String baseUri = server.getBaseUri();
        String endpoint = server.getEndpoint();

        if (StrUtil.isNotBlank(baseUri)) {
            HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUri);
            if (StrUtil.isNotBlank(endpoint)) {
                builder.endpoint(endpoint);
            }
            McpSyncClient client = McpClient.sync(builder.build()).build();
            client.initialize();
            log.info("MCP client connected via Streamable HTTP to: {}{}", baseUri, endpoint);
            return client;
        }

        if (StrUtil.isBlank(endpoint)) {
            throw new SnailAiException("Streamable HTTP transport requires baseUri or endpoint for MCP server: " + server.getName());
        }

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(endpoint).build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        log.info("MCP client connected via Streamable HTTP to: {}", endpoint);
        return client;
    }

    private McpSyncClient throwDeprecatedSseTransport(String serverName) {
        throw new SnailAiException(SSE_TRANSPORT_DEPRECATED_MESSAGE + serverName);
    }

    /**
     * 清除某个服务器的缓存连接
     */
    public void clearClientCache(Long serverId) {
        McpSyncClient client = clientCache.remove(serverId);
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                log.warn("Failed to close MCP client for server: {}", serverId, e);
            }
        }
    }

    /**
     * 清除所有缓存连接
     */
    public void clearAllClientCache() {
        clientCache.forEach((id, client) -> {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                log.warn("Failed to close MCP client for server: {}", id, e);
            }
        });
        clientCache.clear();
    }
}
