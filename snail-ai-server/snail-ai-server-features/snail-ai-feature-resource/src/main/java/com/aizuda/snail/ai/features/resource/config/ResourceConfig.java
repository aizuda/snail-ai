package com.aizuda.snail.ai.features.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 资源模块配置，绑定 {@code snail-ai.resource.*} 前缀，控制存储后端类型、本地目录与 MinIO 连接。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "snail-ai.resource")
public class ResourceConfig {

    /** 激活的存储后端: LOCAL 或 MINIO */
    private String storageType = "LOCAL";

    /** LOCAL 存储根目录 */
    private String uploadDir = "./upload/resource";

    /** MinIO 连接配置 */
    private MinioConfig minio = new MinioConfig();

    /**
     * MinIO 连接参数
     */
    @Data
    public static class MinioConfig {
        /** 服务端点地址 */
        private String endpoint = "http://localhost:9000";
        /** 访问密钥 */
        private String accessKey = "minioadmin";
        /** 私有密钥 */
        private String secretKey = "minioadmin";
        /** 默认存储桶名称 */
        private String bucket = "snail-ai";
    }
}
