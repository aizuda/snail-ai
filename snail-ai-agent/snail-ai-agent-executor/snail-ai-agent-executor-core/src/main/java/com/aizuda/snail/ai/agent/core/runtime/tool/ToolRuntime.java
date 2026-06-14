package com.aizuda.snail.ai.agent.core.runtime.tool;

import com.aizuda.snail.ai.agent.core.resolver.BaseToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.ClientMcpToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.ClientRagToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.ClientSkillToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.CustomToolCallbackProvider;
import com.aizuda.snail.ai.common.constants.SystemConstants;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端工具运行时，统一解析一次对话所需的工具集合。
 */
@Slf4j
@RequiredArgsConstructor
public class ToolRuntime {

    private static final String TOOL_GROUP_BASE = "base";
    private static final String TOOL_GROUP_RAG = "RAG";
    private static final String TOOL_GROUP_SKILL = "Skill";

    private final ClientSkillToolResolver skillToolResolver;
    private final ClientRagToolResolver ragToolResolver;
    private final BaseToolResolver baseToolResolver;
    private final CustomToolCallbackProvider customToolCallbackProvider;

    public ToolResolution resolve(ChatDispatchRequest dispatchRequest) {
        if (dispatchRequest == null) {
            throw new IllegalArgumentException("ChatDispatchRequest is required");
        }
        ClientMcpToolResolver mcpResolver = new ClientMcpToolResolver();
        List<ToolCallback> tools = new ArrayList<>();

        filterRagMcpDescriptor(dispatchRequest);
        tools.addAll(resolveMcpTools(dispatchRequest.getMcpServers(), mcpResolver));
        addResolvedTools(tools, TOOL_GROUP_BASE, () -> baseToolResolver.resolve(dispatchRequest));
        addResolvedTools(tools, TOOL_GROUP_RAG, () -> ragToolResolver.resolve(dispatchRequest));
        addResolvedTools(tools, TOOL_GROUP_SKILL, () -> skillToolResolver.resolve(dispatchRequest));
        tools.addAll(Arrays.asList(customToolCallbackProvider.getToolCallbacks()));

        return new ToolResolution(tools, mcpResolver);
    }

    private void filterRagMcpDescriptor(ChatDispatchRequest dispatchRequest) {
        List<ChatDispatchRequest.McpServerDescriptor> mcpServers = dispatchRequest.getMcpServers();
        if (mcpServers == null || mcpServers.isEmpty()) {
            return;
        }
        mcpServers.removeIf(server -> SystemConstants.RAG_MCP_SERVER_NAME.equals(server.getName()));
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

    private void addResolvedTools(List<ToolCallback> tools, String name, Supplier<List<ToolCallback>> supplier) {
        try {
            tools.addAll(supplier.get());
        } catch (Exception e) {
            log.warn("Failed to resolve {} tools", name, e);
        }
    }

    @Getter
    public static class ToolResolution implements AutoCloseable {

        private final List<ToolCallback> tools;
        private final ClientMcpToolResolver mcpResolver;

        public ToolResolution(List<ToolCallback> tools, ClientMcpToolResolver mcpResolver) {
            this.tools = tools != null ? List.copyOf(tools) : List.of();
            this.mcpResolver = mcpResolver;
        }

        @Override
        public void close() {
            if (mcpResolver == null) {
                return;
            }
            try {
                mcpResolver.close();
            } catch (Exception e) {
                log.warn("Failed to close MCP tool resolver", e);
            }
        }
    }
}
