package com.aizuda.snail.ai.agent.core.executor.tool;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * ToolCallingManager 构建请求。
 */
@Data
@Builder
public class ToolCallingManagerBuildRequest {
    /**
     * 扩展
     */
    private Map<String, Object> extraBody;
}
