package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;

import java.util.List;

/**
 * 客户端路由策略接口
 */
public interface ClientRouteStrategy {

    /**
     * 从候选实例中选择一个
     *
     * @param candidates 活跃的客户端实例列表
     * @param routeKey   路由键（如 conversationId，用于一致性哈希）
     * @return 选中的实例
     */
    ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey);

    String getType();
}
