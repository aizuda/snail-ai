package com.aizuda.snail.ai.agent.core.resolver;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 客户端记忆检索工具解析器
 * <p>
 * 记忆工具已简化 - 不再使用独立的工具注册，记忆检索通过后台处理
 */
@Slf4j
public class ClientMemoryToolResolver {

    public ClientMemoryToolResolver() {
    }

    public List<ToolCallback> resolve(ChatDispatchRequest request) {
        // Memory tools are now handled by the backend without independent tool registration
        return List.of();
    }
}
