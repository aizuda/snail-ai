package com.aizuda.snail.ai.agent.core.resolver;

import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.agent.core.tool.RagSearchTool;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;

/**
 * 客户端 RAG 知识库搜索工具解析器
 * <p>
 * RAG 工具已简化 - 不再使用独立的工具注册，RAG 搜索功能通过后台处理
 */
@Slf4j
public class ClientRagToolResolver {
    private final RpcClient rpcClient;

    public ClientRagToolResolver(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public List<ToolCallback> resolve(ChatDispatchRequest request) {
        ChatDispatchRequest.AgentConfig agentConfig = request.getAgentConfig();
        if (agentConfig == null
                || !Boolean.TRUE.equals(agentConfig.getRagEnabled())
                || agentConfig.getRagId() == null) {
            return List.of();
        }

        log.info("RAG tool resolved: ragId={}", agentConfig.getRagId());
        return Arrays.asList(ToolCallbacks.from(
                new RagSearchTool(agentConfig.getRagId(), rpcClient)
        ));
    }
}
