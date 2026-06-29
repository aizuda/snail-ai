package com.aizuda.snail.ai.features.resource;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.resource.strategy.ResourceStorageFactory;
import com.aizuda.snail.ai.features.resource.strategy.ResourceStorageService;
import com.aizuda.snail.ai.features.resource.util.MimeTypeUtils;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * 资源服务，统一文件资源的上传（落地存储 + 元数据入库 + 访问地址回填）、加载、删除与查询。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceStorageFactory storageFactory;

    /**
     * 上传资源：写入默认存储后端，登记元数据并回填访问地址
     *
     * @param inputStream  文件输入流
     * @param originalName 原始文件名
     * @param fileSize     文件大小（字节）
     * @param bizType      业务类型
     * @param bizId        业务主键
     * @param creatorId    创建者 ID
     * @return 入库后的资源记录
     */
    @Transactional
    public ResourcePO upload(InputStream inputStream, String originalName, long fileSize,
                             String bizType, Long bizId, Long creatorId) {
        ResourceStorageService storage = storageFactory.getDefault();
        String storageKey = storage.store(bizType, originalName, inputStream);
        String mimeType = MimeTypeUtils.detect(originalName);

        ResourcePO resource = ResourcePO.builder()
                .storageKey(storageKey)
                .originalName(originalName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .storageType(storage.getType())
                .bizType(bizType)
                .bizId(bizId)
                .creatorId(creatorId)
                .build();
        resourceMapper.insert(resource);

        String accessUrl = storage.getAccessUrl(resource.getId(), storageKey);
        resource.setAccessUrl(accessUrl);
        resourceMapper.updateById(resource);

        return resource;
    }

    /**
     * 按资源 ID 加载文件内容流，资源不存在时抛出异常
     *
     * @param resourceId 资源 ID
     * @return 文件输入流
     */
    public InputStream load(Long resourceId) {
        ResourcePO resource = requireResource(resourceId);
        ResourceStorageService storage = storageFactory.get(resource.getStorageType());
        return storage.load(resource.getStorageKey());
    }

    /**
     * 删除资源：先尽力删除存储文件（失败仅告警），再删除元数据记录
     *
     * @param resourceId 资源 ID
     */
    @Transactional
    public void delete(Long resourceId) {
        ResourcePO resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            return;
        }
        try {
            ResourceStorageService storage = storageFactory.get(resource.getStorageType());
            storage.delete(resource.getStorageKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}", resource.getStorageKey(), e);
        }
        resourceMapper.deleteById(resourceId);
    }

    /**
     * 按 ID 查询资源记录
     *
     * @param resourceId 资源 ID
     * @return 资源记录，不存在时返回 {@code null}
     */
    public ResourcePO getById(Long resourceId) {
        return resourceMapper.selectById(resourceId);
    }

    /**
     * 按存储键查询资源记录
     *
     * @param storageKey 存储键
     * @return 资源记录，不存在时返回 {@code null}
     */
    public ResourcePO getByStorageKey(String storageKey) {
        return resourceMapper.selectOne(new LambdaQueryWrapper<ResourcePO>()
                .eq(ResourcePO::getStorageKey, storageKey));
    }

    /**
     * 调整资源的 bizType（例如：预览阶段 DOCUMENT_PREVIEW → 提交后 DOCUMENT）。
     */
    public void updateBizType(Long resourceId, String bizType) {
        ResourcePO resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            return;
        }
        resource.setBizType(bizType);
        resourceMapper.updateById(resource);
        log.info("资源 bizType 变更 resourceId={} -> bizType={}", resourceId, bizType);
    }

    /**
     * 按 ID 获取资源，不存在时抛出异常
     *
     * @param resourceId 资源 ID
     * @return 资源记录（保证非空）
     */
    public ResourcePO requireResource(Long resourceId) {
        ResourcePO r = resourceMapper.selectById(resourceId);
        if (r == null) {
            throw new SnailAiException("Resource not found: " + resourceId);
        }
        return r;
    }
}
