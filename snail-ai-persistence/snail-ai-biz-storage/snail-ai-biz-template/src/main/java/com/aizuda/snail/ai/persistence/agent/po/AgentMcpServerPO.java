package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent与MCP服务器关联持久化对象
 * 表: snail_ai_agent_mcp_server
 *
 * 表示Agent与MCP Server的多对多关系
 * 一个Agent可以绑定多个MCP Server，增强Agent的能力
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_agent_mcp_server")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentMcpServerPO {

    /**
     * 关联ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID (外键)
     * 关联到 snail_ai_agent.id
     */
    private Long agentId;

    /**
     * MCP服务器ID (外键)
     * 关联到 snail_ai_mcp_server.id
     */
    private Long mcpServerId;

    /**
     * 创建时间
     * 关联关系创建的时刻
     */
    private LocalDateTime createDt;
}
