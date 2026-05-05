package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 固定路由策略（始终选第一个实例，适用于单客户端场景）
 *
 * @author opensnail
 * @date 2025-04-09
 */
@Component
public class FirstClientRoute implements ClientRouteStrategy {

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        return candidates.get(0);
    }

    @Override
    public String getType() {
        return RouteStrategyType.FIRST;
    }
}
