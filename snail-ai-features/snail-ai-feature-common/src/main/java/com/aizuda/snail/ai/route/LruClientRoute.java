package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LRU 路由策略（最近最少使用）
 *
 * @author opensnail
 * @date 2025-04-09
 */
@Component
public class LruClientRoute implements ClientRouteStrategy {

    private static final int MAX_CACHE_SIZE = 100;

    private final LinkedHashMap<String, String> accessOrder = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        synchronized (accessOrder) {
            String lastUsedHostId = accessOrder.get(routeKey);

            for (ClientInstanceInfo c : candidates) {
                if (!c.getHostId().equals(lastUsedHostId)) {
                    accessOrder.put(routeKey, c.getHostId());
                    return c;
                }
            }

            ClientInstanceInfo first = candidates.get(0);
            accessOrder.put(routeKey, first.getHostId());
            return first;
        }
    }

    @Override
    public String getType() {
        return RouteStrategyType.LRU;
    }
}
