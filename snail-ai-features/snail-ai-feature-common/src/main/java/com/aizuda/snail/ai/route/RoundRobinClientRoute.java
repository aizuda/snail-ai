package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询路由策略
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Component
public class RoundRobinClientRoute implements ClientRouteStrategy {

    /** 计数器初始值 */
    private static final long COUNTER_INITIAL_VALUE = 0L;

    private final AtomicLong counter = new AtomicLong(COUNTER_INITIAL_VALUE);

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        int index = calculateIndex(candidates.size());
        return candidates.get(index);
    }

    @Override
    public String getType() {
        return RouteStrategyType.ROUND_ROBIN;
    }

    /**
     * 计算轮询索引
     * @param candidateSize 候选实例数量
     * @return 索引值
     */
    private int calculateIndex(int candidateSize) {
        long currentCount = counter.getAndIncrement();
        int index = (int) (currentCount % candidateSize);
        return Math.abs(index);
    }
}
