package com.aizuda.snail.ai.agent.core.executor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * ToolCallback 装饰器：每次 call 时动态注入 traceId 和当前 TOOL observationId 到 ToolContext，
 * 使 MCP Server 端能通过 ToolContext 获取 trace 上下文，将子观测精确挂到对应 TOOL 下。
 */
public class TracingToolCallbackWrapper implements ToolCallback {

    private final ToolCallback delegate;

    public TracingToolCallbackWrapper(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolArguments) {
        return delegate.call(toolArguments);
    }

    @Override
    public String call(String toolArguments, ToolContext toolContext) {
        AgentChatContextHolder.ChatContext ctx = AgentChatContextHolder.getContext();
        if (ctx != null && ctx.getTraceId() != null) {
            Map<String, Object> enriched = new HashMap<>(
                    toolContext != null ? toolContext.getContext() : Map.of());
            enriched.put("traceId", ctx.getTraceId());
            enriched.put("parentToolObservationId", ctx.getCurrentToolObservationId());
            toolContext = new ToolContext(enriched);
        }
        return delegate.call(toolArguments, toolContext);
    }
}
