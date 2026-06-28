package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机路由策略
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Component
public class RandomClientRoute implements ClientRouteStrategy {

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        int randomIndex = generateRandomIndex(candidates.size());
        return candidates.get(randomIndex);
    }

    @Override
    public String getType() {
        return RouteStrategyType.RANDOM;
    }

    /**
     * 生成随机索引
     * @param candidateSize 候选实例数量
     * @return 随机索引
     */
    private int generateRandomIndex(int candidateSize) {
        return ThreadLocalRandom.current().nextInt(candidateSize);
    }
}
