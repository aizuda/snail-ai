package com.aizuda.snail.ai.agent.core.executor.client;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * ChatClient 构建请求。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
public class ChatClientBuildRequest {

    private ChatDispatchRequest dispatchRequest;
    private List<ToolCallback> tools;

    /**
     * 获取分发请求中的模型配置。
     *
     * @return 模型配置；分发请求为空时返回 {@code null}
     */
    public ChatDispatchRequest.ModelConfig getModelConfig() {
        return dispatchRequest != null ? dispatchRequest.getModelConfig() : null;
    }
}
