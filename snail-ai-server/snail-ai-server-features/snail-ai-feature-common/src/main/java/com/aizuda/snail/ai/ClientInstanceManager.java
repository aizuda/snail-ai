package com.aizuda.snail.ai;

import cn.hutool.core.collection.CollUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.app.mapper.AppMapper;
import com.aizuda.snail.ai.persistence.app.mapper.ClientNodeMapper;
import com.aizuda.snail.ai.persistence.app.po.AppPO;
import com.aizuda.snail.ai.persistence.app.po.ClientNodePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * 客户端实例管理器
 * <p>
 * 职责：
 * 1. 维护内存中的活跃实例映射和 gRPC Channel
 * 2. 心跳数据入队，定时批量刷盘（LinkedBlockingDeque 模式）
 * 3. 定时从 DB 拉取远程节点，实现集群间客户端发现
 * 4. 读取时 DB 降级，保证跨节点分发可用
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientInstanceManager {

    // ==================== 常量 ====================

    /** 实例来源 */
    public enum InstanceSource {
        /** 直接心跳到本服务器 */
        LOCAL,
        /** 从 DB 加载（心跳到其他服务器节点） */
        REMOTE
    }

    /** 过期阈值（毫秒），超过此时间未收到心跳的实例视为过期 */
    private static final long EXPIRE_THRESHOLD_MS = 40_000;

    /** DB 行过期偏移量（秒），与 EXPIRE_THRESHOLD_MS 对齐 */
    private static final int EXPIRE_OFFSET_SECONDS = 40;

    /** 注册队列容量上限 */
    private static final int REGISTER_QUEUE_CAPACITY = 1024;

    /** 单次 drain 最大条数 */
    private static final int DRAIN_BATCH_SIZE = 256;

    /** 应用状态：启用 */
    private static final int APP_STATUS_ENABLED = 1;

    // gRPC Channel 参数
    private static final int GRPC_KEEP_ALIVE_TIME_SECONDS = 30;
    private static final int GRPC_KEEP_ALIVE_TIMEOUT_SECONDS = 10;
    private static final int GRPC_IDLE_TIMEOUT_MINUTES = 5;
    private static final int GRPC_MAX_INBOUND_MESSAGE_SIZE = 10 * 1024 * 1024;

    // ==================== 依赖 ====================

    private final AppMapper appMapper;
    private final ClientNodeMapper clientNodeMapper;

    // ==================== 核心数据结构 ====================

    /** 活跃实例缓存，key = "{appId}/{hostId}" */
    private final ConcurrentHashMap<String, ClientInstanceInfo> liveInstances = new ConcurrentHashMap<>();

    /** 心跳注册队列：接收心跳数据，定时批量刷盘到 DB */
    private final LinkedBlockingDeque<ClientRegistration> registerQueue = new LinkedBlockingDeque<>(REGISTER_QUEUE_CAPACITY);

    // ==================== 心跳注册（写入路径） ====================

    /**
     * 注册或更新客户端实例（心跳调用）
     * <p>
     * 1. 更新内存缓存（实时）
     * 2. 入队等待批量刷盘（异步）
     */
    public void registerOrUpdate(ClientRegistration registration) {
        String key = buildInstanceKey(registration.getAppId(), registration.getHostId());

        liveInstances.compute(key, (k, existing) -> {
            if (existing == null) {
                log.info("新客户端注册: appId={}, hostId={}, {}:{}",
                        registration.getAppId(), registration.getHostId(),
                        registration.getHostIp(), registration.getGrpcPort());
                ClientInstanceInfo info = new ClientInstanceInfo(
                        registration.getAppId(), registration.getHostId(),
                        registration.getHostIp(), registration.getGrpcPort());
                applyRegistration(info, registration);
                info.setSource(InstanceSource.LOCAL);
                info.setChannel(createChannel(info.getHostIp(), info.getGrpcPort()));
                return info;
            }

            applyRegistration(existing, registration);
            existing.setSource(InstanceSource.LOCAL);
            ensureChannelAlive(existing);
            return existing;
        });

        // 心跳优先放队头，与 snail-job 一致
        registerQueue.offerFirst(registration);
    }

    private void applyRegistration(ClientInstanceInfo info, ClientRegistration reg) {
        info.setMaxConcurrent(reg.getMaxConcurrent());
        info.setActiveChats(reg.getActiveChats());
        info.setSupportedProviders(reg.getSupportedProviders());
        info.setLabels(reg.getLabels());
        info.setLastHeartbeatTime(System.currentTimeMillis());
    }

    // ==================== 实例查询（读取路径） ====================

    /**
     * 获取指定应用的所有活跃实例
     * <p>
     * 优先从内存缓存读取；若缓存为空则降级查询 DB，
     * 将结果注册到本地缓存后重试（单次重试，无递归风险）。
     */
    public List<ClientInstanceInfo> getAliveInstances(String appId) {
        List<ClientInstanceInfo> cached = getAliveInstancesFromCache(appId);
        if (CollUtil.isNotEmpty(cached)) {
            return cached;
        }

        // DB 降级：查询该 appId 下所有未过期的客户端节点
        List<ClientNodePO> dbNodes = clientNodeMapper.selectList(
                new LambdaQueryWrapper<ClientNodePO>()
                        .eq(ClientNodePO::getAppId, appId)
                        .gt(ClientNodePO::getExpireDt, LocalDateTime.now()));
        if (CollUtil.isEmpty(dbNodes)) {
            return List.of();
        }

        log.info("DB 降级发现 {} 个客户端节点: appId={}", dbNodes.size(), appId);
        for (ClientNodePO node : dbNodes) {
            registerFromDb(node);
        }

        // 重新从缓存获取（已包含 DB 加载的节点）
        return getAliveInstancesFromCache(appId);
    }

    private List<ClientInstanceInfo> getAliveInstancesFromCache(String appId) {
        long now = System.currentTimeMillis();
        return liveInstances.values().stream()
                .filter(i -> i.getAppId().equals(appId))
                .filter(i -> isInstanceAlive(i, now))
                .toList();
    }

    private boolean isInstanceAlive(ClientInstanceInfo info, long currentTime) {
        return (currentTime - info.getLastHeartbeatTime() < EXPIRE_THRESHOLD_MS)
                && info.getChannel() != null
                && !info.getChannel().isShutdown();
    }

    // ==================== DB → 缓存注册 ====================

    /**
     * 将 DB 行注册到本地缓存
     * <p>
     * 核心保护逻辑：
     * - LOCAL 条目不覆盖（本地心跳数据更权威）
     * - REMOTE 条目仅刷新心跳时间（保活）
     * - 新条目创建 gRPC Channel 并标记为 REMOTE
     */
    private void registerFromDb(ClientNodePO node) {
        String key = buildInstanceKey(node.getAppId(), node.getHostId());

        liveInstances.compute(key, (k, existing) -> {
            if (existing != null && existing.getSource() == InstanceSource.LOCAL) {
                // 本地心跳数据更权威，不覆盖
                return existing;
            }

            if (existing != null) {
                // REMOTE 条目：刷新心跳时间 + 更新动态字段
                existing.setLastHeartbeatTime(System.currentTimeMillis());
                existing.setMaxConcurrent(node.getMaxConcurrent() != null ? node.getMaxConcurrent() : existing.getMaxConcurrent());
                existing.setActiveChats(node.getActiveChats() != null ? node.getActiveChats() : existing.getActiveChats());
                ensureChannelAlive(existing);
                return existing;
            }

            // 新建 REMOTE 条目
            ClientInstanceInfo info = new ClientInstanceInfo(
                    node.getAppId(), node.getHostId(),
                    node.getHostIp(), node.getGrpcPort());
            info.setSource(InstanceSource.REMOTE);
            info.setMaxConcurrent(node.getMaxConcurrent() != null ? node.getMaxConcurrent() : 10);
            info.setActiveChats(node.getActiveChats() != null ? node.getActiveChats() : 0);
            info.setSupportedProviders(node.getSupportedProviders());
            info.setLabels(parseLabels(node.getLabels()));
            info.setLastHeartbeatTime(System.currentTimeMillis());
            info.setChannel(createChannel(node.getHostIp(), node.getGrpcPort()));
            log.info("从 DB 加载远程客户端: appId={}, hostId={}, {}:{}",
                    node.getAppId(), node.getHostId(), node.getHostIp(), node.getGrpcPort());
            return info;
        });
    }

    private Map<String, String> parseLabels(String labelsJson) {
        if (labelsJson == null || labelsJson.isBlank()) {
            return null;
        }
        try {
            return JsonUtil.parseHashMap(labelsJson);
        } catch (Exception e) {
            log.warn("解析 labels JSON 失败: {}", labelsJson, e);
            return null;
        }
    }

    // ==================== 定时任务：批量刷盘 ====================

    /**
     * 每 5 秒批量将注册队列中的心跳数据刷入 DB
     * <p>
     * 参考 snail-job AbstractRegister.refreshExpireAt() 的批量写入模式：
     * drain 队列 → 按 (appId, hostId) 去重 → 分为 insert/update 两组 → 批量执行
     */
    @Scheduled(fixedDelay = 5_000)
    public void flushRegistrationQueue() {
        List<ClientRegistration> batch = drainQueue();
        if (batch.isEmpty()) {
            return;
        }

        // 按 (appId, hostId) 去重，保留最新的
        Map<String, ClientRegistration> deduped = new LinkedHashMap<>();
        for (ClientRegistration reg : batch) {
            deduped.put(buildInstanceKey(reg.getAppId(), reg.getHostId()), reg);
        }

        List<ClientNodePO> nodes = deduped.values().stream()
                .map(this::toClientNodePO)
                .toList();

        // 查询 DB 中已存在的记录
        Set<String> existingKeys = queryExistingKeys(nodes);

        List<ClientNodePO> inserts = new ArrayList<>();
        List<ClientNodePO> updates = new ArrayList<>();
        for (ClientNodePO node : nodes) {
            String key = buildInstanceKey(node.getAppId(), node.getHostId());
            if (existingKeys.contains(key)) {
                updates.add(node);
            } else {
                inserts.add(node);
            }
        }

        batchInsert(inserts);
        batchUpdate(updates);
    }

    private List<ClientRegistration> drainQueue() {
        ClientRegistration first = registerQueue.poll();
        if (first == null) {
            return List.of();
        }
        List<ClientRegistration> batch = new ArrayList<>();
        batch.add(first);
        registerQueue.drainTo(batch, DRAIN_BATCH_SIZE - 1);
        return batch;
    }

    private ClientNodePO toClientNodePO(ClientRegistration reg) {
        return ClientNodePO.builder()
                .appId(reg.getAppId())
                .hostId(reg.getHostId())
                .hostIp(reg.getHostIp())
                .grpcPort(reg.getGrpcPort())
                .maxConcurrent(reg.getMaxConcurrent())
                .activeChats(reg.getActiveChats())
                .supportedProviders(reg.getSupportedProviders())
                .labels(reg.getLabels() != null ? JsonUtil.toJsonString(reg.getLabels()) : null)
                .expireDt(LocalDateTime.now().plusSeconds(EXPIRE_OFFSET_SECONDS))
                .build();
    }

    private Set<String> queryExistingKeys(List<ClientNodePO> nodes) {
        Set<String> hostIds = new HashSet<>();
        Set<String> appIds = new HashSet<>();
        for (ClientNodePO node : nodes) {
            hostIds.add(node.getHostId());
            appIds.add(node.getAppId());
        }

        List<ClientNodePO> existing = clientNodeMapper.selectList(
                new LambdaQueryWrapper<ClientNodePO>()
                        .select(ClientNodePO::getAppId, ClientNodePO::getHostId)
                        .in(ClientNodePO::getAppId, appIds)
                        .in(ClientNodePO::getHostId, hostIds));

        Set<String> keys = new HashSet<>();
        for (ClientNodePO e : existing) {
            keys.add(buildInstanceKey(e.getAppId(), e.getHostId()));
        }
        return keys;
    }

    private void batchInsert(List<ClientNodePO> inserts) {
        if (inserts.isEmpty()) {
            return;
        }
        try {
            clientNodeMapper.insertBatch(inserts);
        } catch (DuplicateKeyException ignored) {
            // 并发写入导致的重复，忽略
        } catch (Exception e) {
            log.error("批量插入客户端节点失败", e);
        }
    }

    private void batchUpdate(List<ClientNodePO> updates) {
        if (updates.isEmpty()) {
            return;
        }
        try {
            clientNodeMapper.updateBatchExpireAt(updates);
        } catch (Exception e) {
            log.error("批量更新客户端节点失败", e);
        }
    }

    // ===========updateBatchExpireAt========= 定时任务：跨服务器预热 ====================

    /**
     * 每 10 秒从 DB 拉取所有未过期的客户端节点，预热本地缓存
     * <p>
     * 解决集群部署场景：客户端心跳只到一台服务器，其他服务器通过 DB 共享发现。
     * registerFromDb 内部保护 LOCAL 条目不被覆盖。
     */
    @Scheduled(fixedDelay = 10_000)
    public void pullRemoteNodesFromDb() {
        try {
            List<ClientNodePO> aliveNodes = clientNodeMapper.selectList(
                    new LambdaQueryWrapper<ClientNodePO>()
                            .gt(ClientNodePO::getExpireDt, LocalDateTime.now()));
            if (CollUtil.isEmpty(aliveNodes)) {
                return;
            }

            for (ClientNodePO node : aliveNodes) {
                registerFromDb(node);
            }
        } catch (Exception e) {
            log.error("从 DB 拉取远程客户端节点失败", e);
        }
    }

    // ==================== 定时任务：过期清理 ====================

    /**
     * 每 10 秒清理过期的内存实例
     */
    @Scheduled(fixedDelay = 10_000)
    public void expireStaleInstances() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ClientInstanceInfo>> it = liveInstances.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ClientInstanceInfo> entry = it.next();
            ClientInstanceInfo info = entry.getValue();

            if (shouldExpireInstance(info, now)) {
                log.info("客户端过期移除: {} (source={})", entry.getKey(), info.getSource());
                shutdownChannel(info.getChannel());
                it.remove();
            }
        }
    }

    private boolean shouldExpireInstance(ClientInstanceInfo info, long currentTime) {
        return (currentTime - info.getLastHeartbeatTime() > EXPIRE_THRESHOLD_MS)
                || (info.getChannel() != null && info.getChannel().isTerminated());
    }

    /**
     * 每 30 秒清理 DB 中过期的节点行
     */
    @Scheduled(fixedDelay = 30_000)
    public void cleanExpiredNodes() {
        try {
            clientNodeMapper.delete(new LambdaQueryWrapper<ClientNodePO>()
                    .lt(ClientNodePO::getExpireDt, LocalDateTime.now().minusSeconds(EXPIRE_OFFSET_SECONDS)));
        } catch (Exception e) {
            log.error("清理过期客户端节点失败", e);
        }
    }

    // ==================== Token 验证 ====================

    /**
     * 验证 appId + token
     */
    public boolean validateToken(String appId, String token) {
        AppPO app = appMapper.selectOne(
                new LambdaQueryWrapper<AppPO>()
                        .eq(AppPO::getAppId, appId)
                        .eq(AppPO::getStatus, APP_STATUS_ENABLED));
        return app != null && tokenEquals(token, app.getToken());
    }

    private boolean tokenEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 实例移除 ====================

    /**
     * 主动移除实例
     */
    public void removeInstance(String appId, String hostId) {
        String key = buildInstanceKey(appId, hostId);
        ClientInstanceInfo removed = liveInstances.remove(key);
        if (removed != null) {
            shutdownChannel(removed.getChannel());
        }
    }

    // ==================== 工具方法 ====================

    private String buildInstanceKey(String appId, String hostId) {
        return appId + "/" + hostId;
    }

    private void ensureChannelAlive(ClientInstanceInfo info) {
        if (info.getChannel() == null || info.getChannel().isShutdown()) {
            info.setChannel(createChannel(info.getHostIp(), info.getGrpcPort()));
        }
    }

    private ManagedChannel createChannel(String hostIp, int grpcPort) {
        return NettyChannelBuilder.forAddress(hostIp, grpcPort)
                .usePlaintext()
                .keepAliveTime(GRPC_KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS)
                .keepAliveTimeout(GRPC_KEEP_ALIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .idleTimeout(GRPC_IDLE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .maxInboundMessageSize(GRPC_MAX_INBOUND_MESSAGE_SIZE)
                .build();
    }

    private void shutdownChannel(ManagedChannel channel) {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown();
            } catch (Exception e) {
                log.warn("关闭 gRPC Channel 失败", e);
            }
        }
    }

    // ==================== 数据类 ====================

    /**
     * 客户端实例信息
     */
    @Data
    public static class ClientInstanceInfo {
        private final String appId;
        private final String hostId;
        private final String hostIp;
        private final int grpcPort;
        private volatile ManagedChannel channel;
        private volatile int maxConcurrent;
        private volatile int activeChats;
        private volatile String supportedProviders;
        private volatile Map<String, String> labels;
        private volatile long lastHeartbeatTime;
        /** 实例来源：LOCAL=本地心跳，REMOTE=DB 加载 */
        @Getter
        private volatile InstanceSource source;

        public ClientInstanceInfo(String appId, String hostId, String hostIp, int grpcPort) {
            this.appId = appId;
            this.hostId = hostId;
            this.hostIp = hostIp;
            this.grpcPort = grpcPort;
            this.lastHeartbeatTime = System.currentTimeMillis();
            this.source = InstanceSource.LOCAL;
        }
    }

    /**
     * 客户端注册信息（心跳上报的数据）
     */
    @Data
    public static class ClientRegistration {
        private String appId;
        private String hostId;
        private String hostIp;
        private int grpcPort;
        private int maxConcurrent;
        private int activeChats;
        private String supportedProviders;
        private Map<String, String> labels;
    }
}
