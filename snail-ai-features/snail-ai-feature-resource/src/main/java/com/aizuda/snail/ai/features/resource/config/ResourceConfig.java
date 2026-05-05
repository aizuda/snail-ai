package com.aizuda.snail.ai.features.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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

    @Data
    public static class MinioConfig {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "snail-ai";
    }
}
