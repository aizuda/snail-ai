package com.aizuda.snail.ai.features.resource.strategy;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.config.ResourceConfig;
import com.aizuda.snail.ai.features.resource.enums.ResourceStorageTypeEnum;
import com.aizuda.snail.ai.features.resource.util.MimeTypeUtils;
import com.aizuda.snail.ai.features.resource.util.StorageKeyGenerator;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * MinIO 对象存储资源策略，懒加载（双重检查）{@link MinioClient} 并在首次使用时确保存储桶存在。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioResourceStorageService implements ResourceStorageService {

    private static final String FILES_ACCESS_PREFIX = "/files/";

    private final ResourceConfig resourceConfig;
    private volatile MinioClient minioClient;

    @Override
    public String getType() {
        return ResourceStorageTypeEnum.MINIO.getValue();
    }

    @Override
    public String store(String bizType, String fileName, InputStream inputStream) {
        try {
            String storageKey = StorageKeyGenerator.generate(bizType, fileName);
            String contentType = MimeTypeUtils.detect(fileName);
            getClient().putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(storageKey)
                    .stream(inputStream, -1L, 10485760L)
                    .contentType(contentType)
                    .build());
            log.info("Resource stored to MinIO: {}/{}", bucket(), storageKey);
            return storageKey;
        } catch (Exception e) {
            throw new SnailAiException("Failed to store resource to MinIO: " + fileName, e);
        }
    }

    @Override
    public InputStream load(String storageKey) {
        try {
            return getClient().getObject(GetObjectArgs.builder()
                    .bucket(bucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new SnailAiException("Failed to load resource from MinIO: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            getClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket())
                    .object(storageKey)
                    .build());
            log.debug("MinIO 资源删除完成 bucket={} storageKey={}", bucket(), storageKey);
        } catch (Exception e) {
            log.warn("Failed to delete resource from MinIO: {}", storageKey, e);
        }
    }

    @Override
    public String getAccessUrl(Long resourceId, String storageKey) {
        return FILES_ACCESS_PREFIX + storageKey;
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            getClient().statObject(StatObjectArgs.builder()
                    .bucket(bucket())
                    .object(storageKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new SnailAiException("Failed to check resource existence in MinIO", e);
        }
    }

    private String bucket() {
        return resourceConfig.getMinio().getBucket();
    }

    /**
     * 懒加载并返回 MinIO 客户端（双重检查锁），首次创建时校验配置并确保存储桶存在
     *
     * @return 已初始化的 MinIO 客户端
     */
    private MinioClient getClient() {
        if (minioClient == null) {
            synchronized (this) {
                if (minioClient == null) {
                    ResourceConfig.MinioConfig cfg = resourceConfig.getMinio();
                    if (cfg.getEndpoint() == null || cfg.getEndpoint().isBlank()) {
                        throw new IllegalStateException("MinIO is not configured. Set snail-ai.resource.minio.*");
                    }
                    this.minioClient = MinioClient.builder()
                            .endpoint(cfg.getEndpoint())
                            .credentials(cfg.getAccessKey(), cfg.getSecretKey())
                            .build();
                    ensureBucketExists();
                    log.info("MinIO 客户端初始化完成 endpoint={} bucket={}", cfg.getEndpoint(), bucket());
                }
            }
        }
        return minioClient;
    }

    private void ensureBucketExists() {
        try {
            String b = bucket();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(b).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(b).build());
                log.info("MinIO bucket created: {}", b);
            }
        } catch (Exception e) {
            throw new SnailAiException("Failed to ensure MinIO bucket exists", e);
        }
    }
}
