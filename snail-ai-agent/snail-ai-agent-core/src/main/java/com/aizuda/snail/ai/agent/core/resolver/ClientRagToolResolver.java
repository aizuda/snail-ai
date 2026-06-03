package com.aizuda.snail.ai.agent.core.resolver;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.agent.core.tool.RagSearchTool;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.enums.agent.RagCallModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;

/**
 * 客户端 RAG 知识库搜索工具解析器
 * <p>
 * 仅在智能调用模式下注册 rag_search tool；
 * 强制调用模式由服务端预检索，不需要 tool。
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
                || StrUtil.isBlank(agentConfig.getRagIds())) {
            return List.of();
        }

        // 强制调用模式不注册 tool（服务端已预检索注入 prompt）
        if (RagCallModeEnum.FORCED.getMode() == agentConfig.getRagCallMode()) {
            return List.of();
        }

        List<Long> ragIds = Arrays.stream(agentConfig.getRagIds().split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::parseLong)
                .toList();

        log.info("RAG tool resolved (smart mode): ragIds={}", ragIds);
        return Arrays.asList(ToolCallbacks.from(
                new RagSearchTool(ragIds, rpcClient)
        ));
    }
}
