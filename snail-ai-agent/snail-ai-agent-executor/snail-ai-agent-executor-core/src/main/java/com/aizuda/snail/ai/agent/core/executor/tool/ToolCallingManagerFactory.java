package com.aizuda.snail.ai.agent.core.executor.tool;

import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * ToolCallingManager 构建工厂。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ToolCallingManagerFactory {

    /**
     * 根据构建请求创建 {@link ToolCallingManager}。
     *
     * @param request ToolCallingManager 构建请求
     * @return 构建完成的 ToolCallingManager
     */
    ToolCallingManager build(ToolCallingManagerBuildRequest request);
}
