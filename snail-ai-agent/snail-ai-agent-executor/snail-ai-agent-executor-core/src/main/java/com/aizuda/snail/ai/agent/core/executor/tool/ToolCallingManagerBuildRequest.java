package com.aizuda.snail.ai.agent.core.executor.tool;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** ToolCallingManager 构建请求。 */
@Data
@Builder
public class ToolCallingManagerBuildRequest {
    /** 扩展 */
    private Map<String, Object> extraBody;
}
