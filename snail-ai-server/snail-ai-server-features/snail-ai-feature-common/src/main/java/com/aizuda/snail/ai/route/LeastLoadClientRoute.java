package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 最少负载路由（默认策略）
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Component
public class LeastLoadClientRoute implements ClientRouteStrategy {

    /** 零并发阈值 */
    private static final int ZERO_CONCURRENT_THRESHOLD = 0;

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        return candidates.stream()
                .min(Comparator.comparingDouble(this::calculateLoadRatio))
                .orElseGet(() -> candidates.isEmpty() ? null : candidates.get(0));
    }

    @Override
    public String getType() {
        return RouteStrategyType.LEAST_LOAD;
    }

    /**
     * 计算负载比率
     * @param client 客户端实例
     * @return 负载比率，0-1之间表示正常负载，Double.MAX_VALUE 表示无效实例
     */
    private double calculateLoadRatio(ClientInstanceInfo client) {
        return client.getMaxConcurrent() > ZERO_CONCURRENT_THRESHOLD
                ? (double) client.getActiveChats() / client.getMaxConcurrent()
                : Double.MAX_VALUE;
    }
}
