package com.aizuda.snail.ai.agent.core.resolver;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 客户端 RAG 知识库搜索工具解析器
 * <p>
 * RAG 工具已简化 - 不再使用独立的工具注册，RAG 搜索功能通过后台处理
 */
@Slf4j
public class ClientRagToolResolver {

    public ClientRagToolResolver() {
    }

    public List<ToolCallback> resolve(ChatDispatchRequest request) {
        // RAG tools are now handled by the backend without independent tool registration
        return List.of();
    }
}
