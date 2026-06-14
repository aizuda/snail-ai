package com.aizuda.snail.ai.agent.core.executor.client;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * ChatClient 构建请求。
 */
@Data
@Builder
public class ChatClientBuildRequest {

    private ChatDispatchRequest dispatchRequest;
    private List<ToolCallback> tools;

    public ChatDispatchRequest.ModelConfig getModelConfig() {
        return dispatchRequest != null ? dispatchRequest.getModelConfig() : null;
    }
}
