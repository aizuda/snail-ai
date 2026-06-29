package com.aizuda.snail.ai.features.resource.strategy;

import com.aizuda.snail.ai.features.resource.config.ResourceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资源存储策略工厂，按存储类型从所有 {@link ResourceStorageService} 实现中选取对应策略（本地 / MinIO 等）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class ResourceStorageFactory {

    private final List<ResourceStorageService> storageServices;
    private final ResourceConfig resourceConfig;

    /**
     * 按存储类型获取对应的存储策略
     *
     * @param type 存储类型标识（如 LOCAL、MINIO）
     * @return 匹配的存储策略，找不到时抛出异常
     */
    public ResourceStorageService get(String type) {
        return storageServices.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported resource storage type: " + type));
    }

    /**
     * 获取配置指定的默认存储策略
     *
     * @return 默认存储策略
     */
    public ResourceStorageService getDefault() {
        return get(resourceConfig.getStorageType());
    }
}
