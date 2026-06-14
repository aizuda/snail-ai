package com.aizuda.snail.ai.agent.core.executor.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * 默认 ToolCallingManager 工厂。
 */
@RequiredArgsConstructor
public class DefaultToolCallingManagerFactory implements ToolCallingManagerFactory {

    @Override
    public ToolCallingManager build(ToolCallingManagerBuildRequest request) {
        return ToolCallingManager.builder().build();
    }
}
