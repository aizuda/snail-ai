package com.aizuda.snail.ai.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "snail-ai")
public class SnailAiAgentProperties {

    private boolean enabled = true;

    /** 应用 ID */
    private String appId;

    /** 认证令牌 */
    private String token;

    /** 指定客户端端口 */
    private int port = 17889;

    /**
     * 指定客户端IP，默认取本地IP
     */
    private String host;

    /** 最大并发对话数 */
    private int maxConcurrentChats = 10;

    /** Skill 文件临时目录 */
    private String skillTempDir = "/tmp/snail-ai-agent/skills";

    /** shell 文件临时目录 */
    private String shellTempDir = "/tmp/snail-ai-agent/script/";

    /**
     * 服务端配置
     */
    private ServerConfig server = new ServerConfig();

    @Data
    public static class ServerConfig {
        /**
         * 服务端的地址，若服务端集群部署则此处配置域名
         */
        private String host = "127.0.0.1";

        /**
         * 服务端 rpc 的端口号
         */
        private int port = 18888;

        /** 超时时间（毫秒） */
        private long timeout = 5000;

        /** 重试次数 */
        private int retryTimes = 3;

        /** 重试间隔（毫秒） */
        private int retryInterval = 1000;
    }

}
