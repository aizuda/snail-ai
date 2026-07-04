package com.aizuda.snail.ai.admin.vo.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP 服务端视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class McpServerResponseVO {

    /** 服务端 ID */
    private Long id;

    /** 服务端名称 */
    private String name;

    /** 服务端描述 */
    private String description;

    /** 传输类型 */
    private Integer transportType;

    /** 基础地址 */
    private String baseUri;

    /** 端点路径 */
    private String endpoint;

    /** Stdio 启动命令 */
    private String command;

    /** Stdio 命令参数 */
    private List<String> args;

    /** Stdio 环境变量 */
    private Map<String, String> envVars;

    /** 超时时间（毫秒） */
    private Long timeoutMs;

    /** Streamable HTTP 请求头 */
    private Map<String, String> headers;

    /** 最近连接时间 */
    private LocalDateTime lastConnectDt;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
