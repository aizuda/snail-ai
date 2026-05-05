package com.aizuda.snail.ai.features.resource.strategy;

import com.aizuda.snail.ai.features.resource.config.ResourceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResourceStorageFactory {

    private final List<ResourceStorageService> storageServices;
    private final ResourceConfig resourceConfig;

    public ResourceStorageService get(String type) {
        return storageServices.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported resource storage type: " + type));
    }

    public ResourceStorageService getDefault() {
        return get(resourceConfig.getStorageType());
    }
}
