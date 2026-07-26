package com.aizuda.snail.ai.agent.core.resolver;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.constants.McpDefaults;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.enums.mcp.McpTransportTypeEnum;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.*;

/**
 * 客户端 MCP 工具解析器
 * <p>
 * 从 dispatch 数据中的 mcpServers 描述符列表连接 MCP 服务并返回 ToolCallback。
 *
 * @author opensnail
 * @since 2025-04-08
 */
@Slf4j
public class ClientMcpToolResolver {

    private static final String SSE_TRANSPORT_DEPRECATED_MESSAGE =
            "MCP SSE transport is deprecated in MCP SDK 2.x, please use Streamable HTTP for MCP server: {}";

    private final List<McpSyncClient> connectedClients = new ArrayList<>();

    /**
     * 解析 MCP 描述符列表，连接 MCP 服务并返回工具回调。
     *
     * @param mcpServers MCP 服务描述符列表
     * @return 已连接 MCP 服务的工具回调列表（连接失败的服务将被跳过）
     */
    public List<ToolCallback> resolve(List<ChatDispatchRequest.McpServerDescriptor> mcpServers) {
        List<ToolCallback> callbacks = new ArrayList<>();

        if (mcpServers == null || mcpServers.isEmpty()) {
            return callbacks;
        }

        for (ChatDispatchRequest.McpServerDescriptor desc : mcpServers) {
            try {
                connectAndRegisterServer(desc, callbacks);
            } catch (Exception e) {
                log.warn("Failed to connect MCP server: {}", desc.getName(), e);
            }
        }

        return callbacks;
    }

    /**
     * 关闭所有连接
     * todo 这里需要做下缓存，没必须每次都关闭
     */
    public void close() {
        for (McpSyncClient client : connectedClients) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Failed to close MCP client", e);
            }
        }
        connectedClients.clear();
    }

    private void connectAndRegisterServer(ChatDispatchRequest.McpServerDescriptor desc,
                                          List<ToolCallback> callbacks) {
        Integer transportType = desc.getTransportType() != null
                ? desc.getTransportType()
                : McpTransportTypeEnum.STREAMABLE_HTTP.getValue();
        McpTransportTypeEnum transportEnum = McpTransportTypeEnum.fromValue(transportType);
        if (transportEnum == null) {
            log.warn("Unsupported MCP transport: {}", transportType);
            return;
        }

        McpSyncClient client = connectMcpServer(desc, transportEnum);
        if (client == null) {
            return;
        }
        connectedClients.add(client);
        registerToolCallbacks(client, callbacks);
        log.info("MCP server connected: name={}, type={}", desc.getName(), transportEnum.getProtocol());
    }

    private void registerToolCallbacks(McpSyncClient client, List<ToolCallback> callbacks) {
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(List.of(client))
                .build();
        callbacks.addAll(Arrays.asList(provider.getToolCallbacks()));
    }

    private McpSyncClient connectMcpServer(ChatDispatchRequest.McpServerDescriptor desc,
                                           McpTransportTypeEnum transportType) {
        return switch (transportType) {
            case SSE -> {
                log.warn(SSE_TRANSPORT_DEPRECATED_MESSAGE, desc.getName());
                yield null;
            }
            case STREAMABLE_HTTP -> connectStreamableHttp(desc);
            case STDIO -> connectStdioClient(desc);
        };
    }

    private McpSyncClient connectStreamableHttp(ChatDispatchRequest.McpServerDescriptor desc) {
        String baseUri = desc.getBaseUri();
        String endpoint = desc.getEndpoint();

        if (StrUtil.isNotBlank(baseUri)) {
            HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUri);
            configureStreamableHttp(builder, desc);
            if (StrUtil.isNotBlank(endpoint)) {
                builder.endpoint(endpoint);
            }
            McpSyncClient client = buildClient(builder.build(), desc.getTimeoutMs());
            client.initialize();
            return client;
        }

        if (StrUtil.isBlank(endpoint)) {
            log.warn("Streamable HTTP endpoint is blank for server: {}", desc.getName());
            return null;
        }

        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(endpoint);
        configureStreamableHttp(builder, desc);
        McpSyncClient client = buildClient(builder.build(), desc.getTimeoutMs());
        client.initialize();
        return client;
    }

    private void configureStreamableHttp(HttpClientStreamableHttpTransport.Builder builder,
                                         ChatDispatchRequest.McpServerDescriptor desc) {
        Map<String, String> headers = desc.getHeaders();
        Duration timeout = toTimeoutDuration(desc.getTimeoutMs());
        builder.connectTimeout(timeout)
                .httpRequestCustomizer((requestBuilder, method, uri, body, context) ->
                        applyHeaders(requestBuilder, headers));
    }

    private McpSyncClient connectStdioClient(ChatDispatchRequest.McpServerDescriptor desc) {
        String command = desc.getCommand();
        if (StrUtil.isBlank(command)) {
            log.warn("Stdio command is blank for server: {}", desc.getName());
            return null;
        }

        ServerParameters params = buildServerParameters(desc, command);
        StdioClientTransport transport = createStdioTransport(params);
        McpSyncClient client = buildClient(transport, desc.getTimeoutMs());
        client.initialize();
        return client;
    }

    private McpSyncClient buildClient(McpClientTransport transport, Long timeoutMs) {
        Duration timeout = toTimeoutDuration(timeoutMs);
        return McpClient.sync(transport)
                .requestTimeout(timeout)
                .initializationTimeout(timeout)
                .build();
    }

    private Duration toTimeoutDuration(Long timeoutMs) {
        if (timeoutMs == null || timeoutMs <= 0) {
            return Duration.ofMillis(McpDefaults.DEFAULT_TIMEOUT_MS);
        }
        return Duration.ofMillis(timeoutMs);
    }

    private void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach(requestBuilder::header);
    }

    private ServerParameters buildServerParameters(ChatDispatchRequest.McpServerDescriptor desc, String command) {
        ServerParameters.Builder paramsBuilder = ServerParameters.builder(command);

        addArgsIfPresent(paramsBuilder, desc.getArgs());
        addEnvVarsIfPresent(paramsBuilder, desc.getEnvVars());

        return paramsBuilder.build();
    }

    private void addArgsIfPresent(ServerParameters.Builder paramsBuilder, List<String> args) {
        if (args != null && !args.isEmpty()) {
            paramsBuilder.args(args);
        }
    }

    private void addEnvVarsIfPresent(ServerParameters.Builder paramsBuilder, Map<String, String> envVars) {
        Map<String, String> merged = new java.util.LinkedHashMap<>();
        String systemPath = System.getenv("PATH");
        if (systemPath != null && !systemPath.isBlank()) {
            merged.put("PATH", systemPath);
        }
        if (envVars != null && !envVars.isEmpty()) {
            merged.putAll(envVars);
        }
        if (!merged.isEmpty()) {
            paramsBuilder.env(merged);
        }
    }

    private StdioClientTransport createStdioTransport(ServerParameters params) {
        JacksonMcpJsonMapperSupplier supplier = new JacksonMcpJsonMapperSupplier();
        McpJsonMapper jsonMapper = supplier.get();
        return new StdioClientTransport(params, jsonMapper);
    }
}
