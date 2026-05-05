package com.aizuda.snail.ai.openapi.client.core.config;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import lombok.Data;

/**
 * OpenAPI 客户端配置属性
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Data
public class SnailAiOpenApiProperties {

    private boolean enabled = false;

    /**
     * 默认可以不配置，不配置则取{@link SnailAiAgentProperties#getServerHost()}
     */
    private String serverHost;
    /**
     * 默认是8080 对应 server.port: 8080
     */
    private int webPort = 8080;
    /**
     * 是否是https协议
     */
    private boolean https;
    /**
     * 公共前缀
     */
    private String prefix = "snail-ai";

    private long connectTimeoutMs = 5000;
    private long readTimeoutMs = 60000;
    private long chatTimeoutMs = 300000;
}
