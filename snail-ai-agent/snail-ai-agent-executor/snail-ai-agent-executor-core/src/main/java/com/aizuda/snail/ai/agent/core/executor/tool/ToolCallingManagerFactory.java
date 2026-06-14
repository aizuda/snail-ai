package com.aizuda.snail.ai.agent.core.executor.tool;

import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * ToolCallingManager 构建工厂。
 */
public interface ToolCallingManagerFactory {

    ToolCallingManager build(ToolCallingManagerBuildRequest request);
}
