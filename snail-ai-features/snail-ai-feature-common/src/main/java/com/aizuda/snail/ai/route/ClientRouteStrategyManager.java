package com.aizuda.snail.ai.route;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由策略管理器
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Component
@RequiredArgsConstructor
public class ClientRouteStrategyManager {

    private final List<ClientRouteStrategy> strategies;

    public ClientRouteStrategy get(String type) {
        return strategies.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseGet(this::getDefaultStrategy);
    }

    private ClientRouteStrategy getDefaultStrategy() {
        return strategies.stream()
                .filter(s -> RouteStrategyType.LEAST_LOAD.equals(s.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No route strategy available"));
    }
}
