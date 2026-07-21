package com.aizuda.snail.ai.admin.service.mcp;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.constants.McpDefaults;
import com.aizuda.snail.ai.common.enums.mcp.McpTransportTypeEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.service.McpToolService;
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
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpClientTransport;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP 服务管理服务，负责 MCP 服务器配置的增删改查、连通性测试与智能体 MCP 关联。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private static final String SSE_TRANSPORT_DEPRECATED_MESSAGE =
            "MCP SSE transport is deprecated in MCP SDK 2.x, please use Streamable HTTP for MCP server: ";

    private final McpServerMapper mcpServerMapper;
    private final AgentMcpServerMapper agentMcpServerMapper;
    private final McpToolService mcpToolService;

    /**
     * 分页查询 MCP 服务
     *
     * @param query 分页与过滤条件（关键字、传输类型、时间区间）
     * @return MCP 服务分页结果
     */
    public PageResult<List<McpServerResponseVO>> page(McpServerQueryVO query) {
        LambdaQueryWrapper<McpServerPO> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StrUtil.isNotBlank(query.getKeyword()), McpServerPO::getName, query.getKeyword())
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
     *
     * @param id MCP 服务 ID
     * @return MCP 服务详情 VO；不存在时抛出异常
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
     *
     * @param request MCP 服务创建请求参数
     * @return 创建成功的 MCP 服务 VO
     */
    public McpServerResponseVO create(McpServerRequestVO request) {
        Integer transportType = request.getTransportType() != null
                ? request.getTransportType()
                : McpTransportTypeEnum.STREAMABLE_HTTP.getValue();
        Long timeoutMs = normalizeTimeoutMs(request.getTimeoutMs());
        McpServerPO po = McpServerPO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .transportType(transportType)
                .baseUri(request.getBaseUri())
                .endpoint(request.getEndpoint())
                .command(request.getCommand())
                .args(toJson(request.getArgs()))
                .envVars(toJson(request.getEnvVars()))
                .timeoutMs(timeoutMs)
                .headers(toJson(isStreamableHttp(transportType) ? normalizeHeaders(request.getHeaders()) : null))
                .creatorId(UserSessionUtils.currentUserSession().getId())
                .build();
        mcpServerMapper.insert(po);
        log.info("创建 MCP 服务成功 id={} name={} transportType={}", po.getId(), po.getName(), po.getTransportType());
        return toResponseVO(po);
    }

    /**
     * 更新 MCP 服务
     *
     * @param id      MCP 服务 ID
     * @param request MCP 服务更新请求参数（仅非空字段生效）
     * @return 更新后的 MCP 服务 VO
     */
    public McpServerResponseVO update(Long id, McpServerRequestVO request) {
        McpServerPO po = mcpServerMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("MCP Server not found: " + id);
        }

        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getTransportType() != null) {
            po.setTransportType(request.getTransportType());
            po.setBaseUri(null);
            po.setEndpoint(null);
            po.setCommand(null);
            po.setArgs(null);
            po.setEnvVars(null);
            po.setHeaders(null);
        }
        if (request.getBaseUri() != null) {
            po.setBaseUri(request.getBaseUri());
        }
        if (request.getEndpoint() != null) {
            po.setEndpoint(request.getEndpoint());
        }
        if (request.getCommand() != null) {
            po.setCommand(request.getCommand());
        }
        if (request.getArgs() != null) {
            po.setArgs(toJson(request.getArgs()));
        }
        if (request.getEnvVars() != null) {
            po.setEnvVars(toJson(request.getEnvVars()));
        }
        if (request.getTimeoutMs() != null) {
            po.setTimeoutMs(normalizeTimeoutMs(request.getTimeoutMs()));
        }
        if (request.getHeaders() != null) {
            po.setHeaders(toJson(isStreamableHttp(po.getTransportType()) ? normalizeHeaders(request.getHeaders()) : null));
        }

        mcpServerMapper.updateById(po);
        mcpToolService.clearClientCache(id);
        log.info("更新 MCP 服务成功 id={} name={}", id, po.getName());
        return toResponseVO(po);
    }

    /**
     * 删除 MCP 服务（仍被智能体引用时拒绝删除）
     *
     * @param id MCP 服务 ID
     */
    @Transactional
    public void delete(Long id) {
        long cnt = agentMcpServerMapper.selectCount(
                new LambdaQueryWrapper<AgentMcpServerPO>().eq(AgentMcpServerPO::getMcpServerId, id));
        if (cnt > 0) {
            throw new SnailAiException("该MCP服务仍被" + cnt + "个智能体使用，无法删除");
        }
        mcpServerMapper.deleteById(id);
        mcpToolService.clearClientCache(id);
        log.info("删除 MCP 服务成功 id={}", id);
    }

    /**
     * 测试连接
     *
     * @param id MCP 服务 ID
     * @return 连接是否成功
     */
    public boolean testConnection(Long id) {
        McpServerPO po = mcpServerMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("MCP Server not found: " + id);
        }

        McpSyncClient client = null;
        boolean connected = false;
        try {
            client = createTestClient(po);
            client.initialize();
            verifyServerCapabilities(client);
            connected = true;
            po.setLastConnectDt(LocalDateTime.now());
            log.info("MCP 连接测试成功 id={} name={}", id, po.getName());
        } catch (Exception e) {
            log.warn("MCP 连接测试失败, id={}, name={}, error={}", id, po.getName(), e.getMessage(), e);
        } finally {
            closeTestClient(client, id);
        }
        mcpServerMapper.updateById(po);
        return connected;
    }

    /**
     * 获取所有 MCP 服务（给智能体选择下拉使用）
     *
     * @return 全部 MCP 服务 VO 列表
     */
    public List<McpServerResponseVO> listAll() {
        List<McpServerPO> list = mcpServerMapper.selectList(
                new LambdaQueryWrapper<McpServerPO>().orderByDesc(McpServerPO::getCreateDt));
        return list.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 获取智能体关联的 MCP 服务列表
     *
     * @param agentId 智能体 ID
     * @return 该智能体关联的 MCP 服务 VO 列表
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
        List<McpServerPO> servers = mcpServerMapper.selectByIds(serverIds);
        return servers.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 更新智能体的 MCP 服务关联
     *
     * @param agentId      智能体 ID
     * @param mcpServerIds 目标 MCP 服务 ID 列表（覆盖原关联，可为空表示清空）
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
        log.info("更新智能体 MCP 关联 agentId={} 关联数={}", agentId, mcpServerIds == null ? 0 : mcpServerIds.size());
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
                .timeoutMs(defaultTimeoutMs(po.getTimeoutMs()))
                .headers(parseJsonMap(po.getHeaders()))
                .lastConnectDt(po.getLastConnectDt())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JsonUtil.toJsonString(obj);
    }

    private List<String> parseJsonList(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        return JsonUtil.parseObject(json, new TypeReference<List<String>>() {
        });
    }

    private Map<String, String> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    private Long normalizeTimeoutMs(Long timeoutMs) {
        if (timeoutMs == null) {
            return McpDefaults.DEFAULT_TIMEOUT_MS;
        }
        if (timeoutMs <= 0) {
            throw new SnailAiException("MCP timeoutMs must be greater than 0");
        }
        return timeoutMs;
    }

    private Long defaultTimeoutMs(Long timeoutMs) {
        return timeoutMs == null ? McpDefaults.DEFAULT_TIMEOUT_MS : timeoutMs;
    }

    private Duration toTimeoutDuration(Long timeoutMs) {
        return Duration.ofMillis(defaultTimeoutMs(timeoutMs));
    }

    private boolean isStreamableHttp(Integer transportType) {
        Integer actualType = transportType == null ? McpTransportTypeEnum.STREAMABLE_HTTP.getValue() : transportType;
        return McpTransportTypeEnum.STREAMABLE_HTTP.getValue().equals(actualType);
    }

    private Map<String, String> normalizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        Map<String, String> normalizedHeaders = new LinkedHashMap<>();
        Set<String> headerNames = new HashSet<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey() == null ? null : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            validateHeader(key, value, headerNames);
            normalizedHeaders.put(key, value);
        }
        return normalizedHeaders.isEmpty() ? null : normalizedHeaders;
    }

    private void validateHeader(String key, String value, Set<String> headerNames) {
        if (StrUtil.isBlank(key)) {
            throw new SnailAiException("MCP header key cannot be blank");
        }
        if (containsLineBreak(key) || containsLineBreak(value)) {
            throw new SnailAiException("MCP header key/value cannot contain line breaks");
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if (!headerNames.add(normalizedKey)) {
            throw new SnailAiException("Duplicate MCP header key: " + key);
        }
    }

    private boolean containsLineBreak(String value) {
        return value != null && (value.contains("\r") || value.contains("\n"));
    }

    private McpSyncClient createTestClient(McpServerPO server) {
        Integer transportType = server.getTransportType() != null
                ? server.getTransportType()
                : McpTransportTypeEnum.STREAMABLE_HTTP.getValue();
        McpTransportTypeEnum transportEnum = McpTransportTypeEnum.fromValue(transportType);
        if (transportEnum == null) {
            throw new SnailAiException("Unsupported MCP transport type: " + transportType);
        }

        return switch (transportEnum) {
            case SSE -> throwDeprecatedSseTransport(server.getName());
            case STREAMABLE_HTTP -> createStreamableHttpClient(server);
            case STDIO -> createStdioClient(server);
        };
    }

    private McpSyncClient throwDeprecatedSseTransport(String serverName) {
        throw new SnailAiException(SSE_TRANSPORT_DEPRECATED_MESSAGE + serverName);
    }

    private McpSyncClient createStreamableHttpClient(McpServerPO server) {
        String baseUri = server.getBaseUri();
        String endpoint = server.getEndpoint();

        if (StrUtil.isNotBlank(baseUri)) {
            HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUri);
            configureStreamableHttp(builder, server);
            if (StrUtil.isNotBlank(endpoint)) {
                builder.endpoint(endpoint);
            }
            return buildClient(builder.build(), server.getTimeoutMs());
        }

        if (StrUtil.isBlank(endpoint)) {
            throw new SnailAiException("Streamable HTTP transport requires baseUri or endpoint for MCP server: "
                    + server.getName());
        }

        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(endpoint);
        configureStreamableHttp(builder, server);
        return buildClient(builder.build(), server.getTimeoutMs());
    }

    private void configureStreamableHttp(HttpClientStreamableHttpTransport.Builder builder, McpServerPO server) {
        Map<String, String> headers = parseJsonMap(server.getHeaders());
        Duration timeout = toTimeoutDuration(server.getTimeoutMs());
        builder.connectTimeout(timeout)
                .httpRequestCustomizer((requestBuilder, method, uri, body, context) ->
                        applyHeaders(requestBuilder, headers));
    }

    private void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach(requestBuilder::header);
    }

    private McpSyncClient createStdioClient(McpServerPO server) {
        String command = server.getCommand();
        if (StrUtil.isBlank(command)) {
            throw new SnailAiException("Stdio transport requires command for MCP server: " + server.getName());
        }

        ServerParameters.Builder paramsBuilder = ServerParameters.builder(command);
        addArgs(paramsBuilder, server.getArgs());
        addEnvVars(paramsBuilder, server.getEnvVars());

        JacksonMcpJsonMapperSupplier supplier = new JacksonMcpJsonMapperSupplier();
        McpJsonMapper jsonMapper = supplier.get();
        return buildClient(new StdioClientTransport(paramsBuilder.build(), jsonMapper), server.getTimeoutMs());
    }

    private McpSyncClient buildClient(McpClientTransport transport, Long timeoutMs) {
        Duration timeout = toTimeoutDuration(timeoutMs);
        return McpClient.sync(transport)
                .requestTimeout(timeout)
                .initializationTimeout(timeout)
                .build();
    }

    private void addArgs(ServerParameters.Builder paramsBuilder, String argsJson) {
        if (StrUtil.isBlank(argsJson)) {
            return;
        }
        List<String> args = JsonUtil.parseObject(argsJson, new TypeReference<List<String>>() {
        });
        if (args != null && !args.isEmpty()) {
            paramsBuilder.args(args);
        }
    }

    private void addEnvVars(ServerParameters.Builder paramsBuilder, String envVarsJson) {
        Map<String, String> envVars = new java.util.LinkedHashMap<>();
        // 自动注入系统 PATH，避免 Windows 下 ProcessBuilder 找不到 npx/node 等命令
        String systemPath = System.getenv("PATH");
        if (systemPath != null && !systemPath.isBlank()) {
            envVars.put("PATH", systemPath);
        }
        if (StrUtil.isNotBlank(envVarsJson)) {
            Map<String, String> parsed = JsonUtil.parseObject(envVarsJson, new TypeReference<Map<String, String>>() {});
            if (parsed != null) {
                envVars.putAll(parsed);
            }
        }
        if (!envVars.isEmpty()) {
            paramsBuilder.env(envVars);
        }
    }

    private void verifyServerCapabilities(McpSyncClient client) {
        McpSchema.ServerCapabilities serverCapabilities = client.getServerCapabilities();
        if (serverCapabilities != null && serverCapabilities.tools() != null) {
            client.listTools();
        }
    }

    private void closeTestClient(McpSyncClient client, Long serverId) {
        if (client == null) {
            return;
        }
        try {
            client.closeGracefully();
        } catch (Exception e) {
            log.warn("关闭 MCP 测试连接失败, id={}", serverId, e);
        }
    }
}
