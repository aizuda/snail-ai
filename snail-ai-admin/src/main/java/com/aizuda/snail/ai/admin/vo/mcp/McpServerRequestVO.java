package com.aizuda.snail.ai.admin.vo.mcp;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class McpServerRequestVO {

    private String name;

    private String description;

    /**
     * 传输类型: sse / streamable_http / stdio
     */
    private Integer transportType;

    private String baseUri;

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

    private Integer authType;

    private Map<String, Object> authConfig;

    private List<String> capabilities;
}
