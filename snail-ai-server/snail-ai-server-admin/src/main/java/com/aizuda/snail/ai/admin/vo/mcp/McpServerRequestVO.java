package com.aizuda.snail.ai.admin.vo.mcp;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务端新增/更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class McpServerRequestVO {

    /** 服务端名称 */
    private String name;

    /** 服务端描述 */
    private String description;

    /**
     * 传输类型: sse / streamable_http / stdio
     */
    private Integer transportType;

    /** 基础地址 */
    private String baseUri;

    /** 端点路径 */
    private String endpoint;

    /**
     * Stdio 命令
     */
    private String command;

    /**
     * Stdio 命令参数
     */
    private List<String> args;

    /**
     * Stdio 环境变量
     */
    private Map<String, String> envVars;

    /** 超时时间（毫秒） */
    private Long timeoutMs;

    /** Streamable HTTP 请求头 */
    private Map<String, String> headers;
}
