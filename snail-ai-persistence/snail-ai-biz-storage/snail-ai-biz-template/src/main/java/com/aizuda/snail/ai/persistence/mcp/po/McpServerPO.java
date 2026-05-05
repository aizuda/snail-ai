package com.aizuda.snail.ai.persistence.mcp.po;

import com.aizuda.snail.ai.common.mcp.McpServerRef;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP (Model Context Protocol) 服务器持久化对象
 * 表: snail_ai_mcp_server
 *
 * 表示一个外部MCP服务器配置
 * 支持多种传输方式：SSE、HTTP流、Stdio进程
 * Agent可通过MCP调用第三方工具和资源
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_mcp_server")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class McpServerPO implements McpServerRef {

    /**
     * MCP服务器ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务器名称
     * 用于前端显示和用户识别
     */
    private String name;

    /**
     * 服务器描述
     * 该MCP服务器的功能说明
     */
    private String description;

    /**
     * 传输类型
     * SSE: 服务器发送事件（单向流）
     * STREAMABLE_HTTP: HTTP流（双向）
     * STDIO: 本地进程/Shell命令
     */
    private Integer transportType;

    /**
     * 服务基础地址
     * 仅当transportType为SSE或STREAMABLE_HTTP时使用
     * 格式: http://host:port
     */
    private String baseUri;

    /**
     * 服务端点路径
     * 仅当transportType为SSE或STREAMABLE_HTTP时使用
     * 格式: /sse 或 /mcp
     */
    private String endpoint;

    /**
     * Stdio命令
     * 仅当transportType为STDIO时使用
     * 可执行的命令或脚本路径
     * 示例: /usr/bin/python, /home/user/mcp-server.sh
     */
    private String command;

    /**
     * Stdio命令参数 (JSON数组格式)
     * 仅当transportType为STDIO时使用
     * 示例: ["-m", "mcp_module", "--debug"]
     */
    private String args;

    /**
     * Stdio环境变量 (JSON对象格式)
     * 仅当transportType为STDIO时使用
     * 执行命令时设置的环境变量
     * 示例: {"API_KEY": "xxx", "LOG_LEVEL": "DEBUG"}
     */
    private String envVars;

    /**
     * MCP协议版本
     * 实现的MCP协议版本号
     * 例如: 1.0, 2.0
     */
    private String version;

    /**
     * 认证类型
     * NONE: 无认证
     * API_KEY: API密钥认证
     * OAUTH2: OAuth2认证
     * CUSTOM: 自定义认证
     */
    private Integer authType;

    /**
     * 认证配置 (JSON格式)
     * 根据authType存储相应的认证信息
     * API_KEY例: {"key": "sk-xxx"}
     * OAUTH2例: {"client_id": "...", "client_secret": "..."}
     */
    private String authConfig;

    /**
     * 服务器状态
     * ACTIVE: 活跃/正常
     * INACTIVE: 非活跃/已禁用
     * ERROR: 错误/连接失败
     * DISCONNECTED: 已断开连接
     */
    private Integer status;

    /**
     * MCP服务器支持的能力 (JSON数组格式)
     * 定义该服务器支持的工具和资源
     * 示例: ["tools", "resources", "prompts"]
     */
    private String capabilities;

    /**
     * 最后连接时间
     * 上次成功连接到MCP服务器的时刻
     * 用于健康检查和监控
     */
    private LocalDateTime lastConnectDt;

    /**
     * 创建者用户ID (外键)
     * 关联到 snail_ai_user.id
     * 该MCP服务器的配置创建人
     */
    private Long creatorId;

    /**
     * 创建时间
     * MCP服务器配置首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * MCP服务器配置最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
