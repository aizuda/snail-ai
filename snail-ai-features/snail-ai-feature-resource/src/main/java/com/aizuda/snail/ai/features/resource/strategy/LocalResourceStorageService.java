package com.aizuda.snail.ai.features.resource.strategy;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.config.ResourceConfig;
import com.aizuda.snail.ai.features.resource.enums.ResourceStorageTypeEnum;
import com.aizuda.snail.ai.features.resource.util.StorageKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalResourceStorageService implements ResourceStorageService {

    private final ResourceConfig resourceConfig;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public String getType() {
        return ResourceStorageTypeEnum.LOCAL.getValue();
    }

    @Override
    public String store(String bizType, String fileName, InputStream inputStream) {
        try {
            String storageKey = StorageKeyGenerator.generate(bizType, fileName);
            Path targetPath = resolveAbsolute(storageKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Resource stored: {}", targetPath);
            return storageKey;
        } catch (IOException e) {
            throw new SnailAiException("Failed to store resource: " + fileName, e);
        }
    }

    @Override
    public InputStream load(String storageKey) {
        try {
            Path path = resolveAbsolute(storageKey);
            if (!Files.exists(path)) {
                throw new SnailAiException("Resource not found: " + storageKey);
            }
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new SnailAiException("Failed to load resource: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveAbsolute(storageKey));
        } catch (IOException e) {
            log.warn("Failed to delete resource: {}", storageKey, e);
        }
    }

    @Override
    public String getAccessUrl(Long resourceId, String storageKey) {
        return contextPath + "/resource/" + resourceId + "/preview";
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveAbsolute(storageKey));
    }

    private Path resolveAbsolute(String storageKey) {
        return Paths.get(resourceConfig.getUploadDir()).resolve(storageKey);
    }
}
