package com.aizuda.snail.ai.agent.core.resolver;

import com.aizuda.snail.ai.agent.core.tool.HttpTool;
import com.aizuda.snail.ai.agent.core.tool.ShellTool;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户端 RAG 知识库搜索工具解析器
 * <p>
 * 当 Agent 启用知识库时，注册本地 {@link RagSearchTool} 替代 MCP 调用。
 */
@Slf4j
public class BaseToolResolver {

    private static final long DEFAULT_SHELL_TIMEOUT_MS = 60000;
    private static final int DEFAULT_SHELL_MAX_OUTPUT_LINES = 500;
    private static final long DEFAULT_HTTP_TIMEOUT_MS = 30000;
    private final String tempDir;

    public BaseToolResolver(String tempDir) {
        this.tempDir = tempDir;
    }

    public List<ToolCallback> resolve(ChatDispatchRequest request) {
        String workingDirectory = buildIsolatedDir(request);
        log.info("ShellTool + HttpTool registered, workingDirectory={}", workingDirectory);
        return new ArrayList<>(Arrays.asList(ToolCallbacks.from(
                new ShellTool(workingDirectory, DEFAULT_SHELL_TIMEOUT_MS, DEFAULT_SHELL_MAX_OUTPUT_LINES),
                new HttpTool(DEFAULT_HTTP_TIMEOUT_MS)
        )));
    }

    /**
     * 构建按 agentId/conversationId 隔离的工作目录，并自动创建
     */
    public String buildIsolatedDir(ChatDispatchRequest request) {
        Long agentId = request.getAgentConfig() != null ? request.getAgentConfig().getAgentId() : null;
        String conversationId = request.getConversationId();
        String dir = tempDir + File.separator
                + (agentId != null ? agentId : "unknown") + File.separator
                + (conversationId != null ? conversationId : "default");
        try {
            Files.createDirectories(Path.of(dir));
        } catch (Exception e) {
            log.warn("Failed to create isolated working directory: {}", dir, e);
        }
        return dir;
    }
}
