package com.aizuda.snail.ai.features.resource.strategy;

import java.io.InputStream;

/**
 * 通用资源存储接口（不绑定任何业务字段）
 */
public interface ResourceStorageService {

    /** 存储类型标识 */
    String getType();

    /**
     * 存储文件并返回存储键
     *
     * @param bizType  业务类型（用于路径分区）
     * @param fileName 原始文件名
     * @param inputStream 文件内容
     * @return 存储键（相对路径或对象 Key）
     */
    String store(String bizType, String fileName, InputStream inputStream);

    /** 加载文件流 */
    InputStream load(String storageKey);

    /** 删除文件 */
    void delete(String storageKey);

    /** 获取访问 URL */
    String getAccessUrl(Long resourceId, String storageKey);

    /** 检查文件是否存在 */
    boolean exists(String storageKey);
}
