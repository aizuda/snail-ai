package com.aizuda.snail.ai.admin.service.mcp;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.enums.mcp.McpAuthTypeEnum;
import com.aizuda.snail.ai.common.enums.mcp.McpConnectionStatusEnum;
import com.aizuda.snail.ai.common.enums.mcp.McpTransportTypeEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMcpServerMapper;
import com.aizuda.snail.ai.persistence.mcp.mapper.McpServerMapper;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.persistence.agent.po.AgentMcpServerPO;
import com.aizuda.snail.ai.persistence.mcp.po.McpServerPO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerQueryVO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerRequestVO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerMapper mcpServerMapper;
    private final AgentMcpServerMapper agentMcpServerMapper;

    /**
     * 分页查询 MCP 服务
     */
    public PageResult<List<McpServerResponseVO>> page(McpServerQueryVO query) {
        LambdaQueryWrapper<McpServerPO> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StrUtil.isNotBlank(query.getKeyword()), McpServerPO::getName, query.getKeyword())
                .eq(ObjUtil.isNotNull(query.getStatus()), McpServerPO::getStatus, query.getStatus())
                .eq(ObjUtil.isNotNull(query.getTransportType()), McpServerPO::getTransportType, query.getTransportType())
                .between(ObjUtil.isNotNull(query.getStartDt()) && ObjUtil.isNotNull(query.getEndDt()),
                        McpServerPO::getCreateDt, query.getStartDt(), query.getEndDt())
                .orderByDesc(McpServerPO::getCreateDt);

        PageDTO<McpServerPO> pageDTO = new PageDTO<>(query.getPage(), query.getSize());
        IPage<McpServerPO> page = mcpServerMapper.selectPage(pageDTO, wrapper);

        List<McpServerResponseVO> records = page.getRecords().stream()
                .map(this::toResponseVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageDTO, records);
    }

    /**
     * 获取 MCP 服务详情
     */
    public McpServerResponseVO getById(Long id) {
        McpServerPO po = mcpServerMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("MCP Server not found: " + id);
        }
        return toResponseVO(po);
    }

    /**
     * 创建 MCP 服务
     */
    public McpServerResponseVO create(McpServerRequestVO request) {
        McpServerPO po = McpServerPO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .transportType(request.getTransportType() != null ? request.getTransportType() : McpTransportTypeEnum.SSE.getValue())
                .baseUri(request.getBaseUri())
                .endpoint(request.getEndpoint())
                .command(request.getCommand())
                .args(toJson(request.getArgs()))
                .envVars(toJson(request.getEnvVars()))
                .version("1.0.0")
                .authType(request.getAuthType() != null ? request.getAuthType() : McpAuthTypeEnum.NONE.getType())
                .authConfig(toJson(request.getAuthConfig()))
                .status(McpConnectionStatusEnum.DISCONNECTED.getValue())
                .capabilities(toJson(request.getCapabilities()))
                .creatorId(UserSessionUtils.currentUserSession().getId())
                .build();
        mcpServerMapper.insert(po);
        return toResponseVO(po);
    }

    /**
     * 更新 MCP 服务
     */
    public McpServerResponseVO update(Long id, McpServerRequestVO request) {
        McpServerPO po = mcpServerMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("MCP Server not found: " + id);
        }

        if (request.getName() != null) po.setName(request.getName());
        if (request.getDescription() != null) po.setDescription(request.getDescription());
        if (request.getTransportType() != null) {
            po.setTransportType(request.getTransportType());
            po.setBaseUri(null);
            po.setEndpoint(null);
            po.setCommand(null);
            po.setArgs(null);
            po.setEnvVars(null);
        }
        if (request.getBaseUri() != null) po.setBaseUri(request.getBaseUri());
        if (request.getEndpoint() != null) po.setEndpoint(request.getEndpoint());
        if (request.getCommand() != null) po.setCommand(request.getCommand());
        if (request.getArgs() != null) po.setArgs(toJson(request.getArgs()));
        if (request.getEnvVars() != null) po.setEnvVars(toJson(request.getEnvVars()));
        if (request.getAuthType() != null) po.setAuthType(request.getAuthType());
        if (request.getAuthConfig() != null) po.setAuthConfig(toJson(request.getAuthConfig()));
        if (request.getCapabilities() != null) po.setCapabilities(toJson(request.getCapabilities()));

        mcpServerMapper.updateById(po);
        return toResponseVO(po);
    }

    /**
     * 删除 MCP 服务
     */
    @Transactional
    public void delete(Long id) {
        // 删除关联关系
        agentMcpServerMapper.delete(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getMcpServerId, id));
        mcpServerMapper.deleteById(id);
    }

    /**
     * 测试连接
     */
    public McpServerResponseVO testConnection(Long id) {
        McpServerPO po = mcpServerMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("MCP Server not found: " + id);
        }

        try {
            // TODO: 实际的 MCP 连接测试逻辑
            po.setStatus(McpConnectionStatusEnum.CONNECTED.getValue());
            po.setLastConnectDt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("MCP 连接测试失败, id={}", id, e);
            po.setStatus(McpConnectionStatusEnum.ERROR.getValue());
        }
        mcpServerMapper.updateById(po);
        return toResponseVO(po);
    }

    /**
     * 获取所有 MCP 服务（给智能体选择下拉使用）
     */
    public List<McpServerResponseVO> listAll() {
        List<McpServerPO> list = mcpServerMapper.selectList(
                new LambdaQueryWrapper<McpServerPO>().orderByDesc(McpServerPO::getCreateDt));
        return list.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 获取智能体关联的 MCP 服务列表
     */
    public List<McpServerResponseVO> getByAgentId(Long agentId) {
        List<AgentMcpServerPO> relations = agentMcpServerMapper.selectList(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getAgentId, agentId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> serverIds = relations.stream()
                .map(AgentMcpServerPO::getMcpServerId)
                .collect(Collectors.toList());
        List<McpServerPO> servers = mcpServerMapper.selectBatchIds(serverIds);
        return servers.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 更新智能体的 MCP 服务关联
     */
    @Transactional
    public void updateAgentMcpServers(Long agentId, List<Long> mcpServerIds) {
        // 先删除旧关联
        agentMcpServerMapper.delete(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getAgentId, agentId));

        // 再插入新关联
        if (mcpServerIds != null && !mcpServerIds.isEmpty()) {
            for (Long serverId : mcpServerIds) {
                AgentMcpServerPO relation = AgentMcpServerPO.builder()
                        .agentId(agentId)
                        .mcpServerId(serverId)
                        .build();
                agentMcpServerMapper.insert(relation);
            }
        }
    }

    // ==================== 私有方法 ====================

    private McpServerResponseVO toResponseVO(McpServerPO po) {
        return McpServerResponseVO.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .transportType(po.getTransportType())
                .baseUri(po.getBaseUri())
                .endpoint(po.getEndpoint())
                .command(po.getCommand())
                .args(parseJsonList(po.getArgs()))
                .envVars(parseJsonMap(po.getEnvVars()))
                .version(po.getVersion())
                .authType(po.getAuthType())
                .status(po.getStatus())
                .capabilities(parseJsonList(po.getCapabilities()))
                .lastConnectDt(po.getLastConnectDt())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        return JsonUtil.toJsonString(obj);
    }

    private List<String> parseJsonList(String json) {
        if (StrUtil.isBlank(json)) return new ArrayList<>();
        return JsonUtil.parseObject(json, new TypeReference<List<String>>() {
        });
    }

    private Map<String, String> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) return null;
        return JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }
}
