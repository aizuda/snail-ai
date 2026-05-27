package com.aizuda.snail.ai.features.resource.strategy;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.config.ResourceConfig;
import com.aizuda.snail.ai.features.resource.enums.ResourceStorageTypeEnum;
import com.aizuda.snail.ai.features.resource.util.MimeTypeUtils;
import com.aizuda.snail.ai.features.resource.util.StorageKeyGenerator;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.Http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioResourceStorageService implements ResourceStorageService {

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
        } catch (Exception e) {
            log.warn("Failed to delete resource from MinIO: {}", storageKey, e);
        }
    }

    @Override
    public String getAccessUrl(Long resourceId, String storageKey) {
        try {
            return getClient().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket())
                    .object(storageKey)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL: {}", storageKey, e);
            return null;
        }
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
