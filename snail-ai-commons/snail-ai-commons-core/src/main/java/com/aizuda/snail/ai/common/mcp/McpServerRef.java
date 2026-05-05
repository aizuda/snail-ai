package com.aizuda.snail.ai.common.mcp;

/**
 * Minimal MCP server view for chat/tool wiring (implemented by persistence PO).
 */
public interface McpServerRef {

    Long getId();

    String getName();

    Integer getTransportType();

    String getBaseUri();

    String getEndpoint();

    String getCommand();

    String getArgs();

    String getEnvVars();

    Integer getAuthType();

    String getAuthConfig();

    Integer getStatus();
}
