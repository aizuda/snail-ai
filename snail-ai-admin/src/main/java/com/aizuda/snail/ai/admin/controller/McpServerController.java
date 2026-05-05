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

@RestController
@RequestMapping("/mcp-server")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<McpServerResponseVO>> page(McpServerQueryVO query) {
        return mcpServerService.page(query);
    }

    @GetMapping("/{id}")
    @LoginRequired
    public Result<McpServerResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.getById(id));
    }

    @PostMapping
    @LoginRequired
    public Result<McpServerResponseVO> create(@RequestBody McpServerRequestVO request) {
        return Result.ok(mcpServerService.create(request));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<McpServerResponseVO> update(@PathVariable("id") Long id,
                                               @RequestBody McpServerRequestVO request) {
        return Result.ok(mcpServerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        mcpServerService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/test-connection")
    @LoginRequired
    public Result<McpServerResponseVO> testConnection(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.testConnection(id));
    }

    @GetMapping("/list")
    @LoginRequired
    public Result<List<McpServerResponseVO>> listAll() {
        return Result.ok(mcpServerService.listAll());
    }
}
