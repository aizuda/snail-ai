package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerQueryVO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerRequestVO;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP 服务管理接口：MCP 服务的分页、CRUD、连通性测试与列表查询。
 * <p>
 * 全部端点均需 ADMIN 角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/mcp-server")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    /** 分页查询 MCP 服务（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<McpServerResponseVO>> page(McpServerQueryVO query) {
        return mcpServerService.page(query);
    }

    /** 按 ID 查询 MCP 服务详情（管理员）。 */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<McpServerResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.getById(id));
    }

    /** 创建 MCP 服务（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<McpServerResponseVO> create(@RequestBody McpServerRequestVO request) {
        return Result.ok(mcpServerService.create(request));
    }

    /** 更新 MCP 服务（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<McpServerResponseVO> update(@PathVariable("id") Long id,
                                               @RequestBody McpServerRequestVO request) {
        return Result.ok(mcpServerService.update(id, request));
    }

    /** 删除 MCP 服务（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        mcpServerService.delete(id);
        return Result.ok(null);
    }

    /** 测试 MCP 服务连通性（管理员）。 */
    @PostMapping("/{id}/test-connection")
    @LoginRequired
    public Result<Boolean> testConnection(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.testConnection(id));
    }

    /** 查询全部 MCP 服务（不分页，管理员）。 */
    @GetMapping("/list")
    @LoginRequired
    public Result<List<McpServerResponseVO>> listAll() {
        return Result.ok(mcpServerService.listAll());
    }
}
