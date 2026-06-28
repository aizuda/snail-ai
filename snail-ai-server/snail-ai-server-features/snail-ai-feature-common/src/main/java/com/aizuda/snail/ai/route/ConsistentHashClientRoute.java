package com.aizuda.snail.ai.route;

import com.aizuda.snail.ai.ClientInstanceManager.ClientInstanceInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希路由（按 conversationId 做会话亲和）
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Component
public class ConsistentHashClientRoute implements ClientRouteStrategy {

    /** 虚拟节点数量 */
    private static final int VIRTUAL_NODES_COUNT = 100;
    
    /** 哈希种子 */
    private static final int HASH_SEED = 31;
    
    /** 哈希掩码（保证非负） */
    private static final long HASH_MASK = 0x7fffffffffffffffL;
    
    /** 虚拟节点分隔符 */
    private static final String VIRTUAL_NODE_SEPARATOR = "#";

    @Override
    public ClientInstanceInfo select(List<ClientInstanceInfo> candidates, String routeKey) {
        TreeMap<Long, ClientInstanceInfo> hashRing = buildHashRing(candidates);
        return selectFromRing(hashRing, routeKey);
    }

    @Override
    public String getType() {
        return RouteStrategyType.CONSISTENT_HASH;
    }

    /**
     * 构建哈希环
     */
    private TreeMap<Long, ClientInstanceInfo> buildHashRing(List<ClientInstanceInfo> candidates) {
        TreeMap<Long, ClientInstanceInfo> ring = new TreeMap<>();
        
        for (ClientInstanceInfo client : candidates) {
            addVirtualNodes(ring, client);
        }
        
        return ring;
    }

    /**
     * 添加虚拟节点
     */
    private void addVirtualNodes(TreeMap<Long, ClientInstanceInfo> ring, ClientInstanceInfo client) {
        for (int i = 0; i < VIRTUAL_NODES_COUNT; i++) {
            String virtualNodeKey = buildVirtualNodeKey(client.getHostId(), i);
            long hash = calculateHash(virtualNodeKey);
            ring.put(hash, client);
        }
    }

    /**
     * 构建虚拟节点键
     */
    private String buildVirtualNodeKey(String hostId, int index) {
        return hostId + VIRTUAL_NODE_SEPARATOR + index;
    }

    /**
     * 从哈希环中选择节点
     */
    private ClientInstanceInfo selectFromRing(TreeMap<Long, ClientInstanceInfo> ring, String routeKey) {
        long keyHash = calculateHash(routeKey);
        SortedMap<Long, ClientInstanceInfo> tailMap = ring.tailMap(keyHash);
        
        return tailMap.isEmpty() 
                ? ring.firstEntry().getValue() 
                : tailMap.get(tailMap.firstKey());
    }

    /**
     * 计算哈希值
     */
    private long calculateHash(String key) {
        long hash = 0;
        for (char c : key.toCharArray()) {
            hash = HASH_SEED * hash + c;
        }
        return hash & HASH_MASK;
    }
}
