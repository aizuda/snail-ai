package com.aizuda.snail.ai.route;

/**
 * 路由策略类型常量
 *
 * @author opensnail
 * @date 2025-04-08
 */
public final class RouteStrategyType {

    /** 最少负载路由 */
    public static final String LEAST_LOAD = "LEAST_LOAD";

    /** 随机路由 */
    public static final String RANDOM = "RANDOM";

    /** 轮询路由 */
    public static final String ROUND_ROBIN = "ROUND_ROBIN";

    /** 一致性哈希路由 */
    public static final String CONSISTENT_HASH = "CONSISTENT_HASH";

    /** LRU 最近最少使用路由 */
    public static final String LRU = "LRU";

    /** 固定路由（始终选第一个） */
    public static final String FIRST = "FIRST";

    private RouteStrategyType() {
        throw new UnsupportedOperationException("Utility class");
    }
}
