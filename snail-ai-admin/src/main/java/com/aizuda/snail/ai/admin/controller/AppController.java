package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.service.app.AppService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.app.*;
import com.aizuda.snail.ai.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping
    @LoginRequired
    public Result<AppResponseVO> create(@RequestBody AppRequestVO request) {
        return Result.ok(appService.create(request));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<AppResponseVO> update(@PathVariable("id") Long id,
                                         @RequestBody AppRequestVO request) {
        return Result.ok(appService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        appService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/toggle-status")
    @LoginRequired
    public Result<Void> toggleStatus(@PathVariable("id") Long id) {
        appService.toggleStatus(id);
        return Result.ok(null);
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<AppResponseVO>> page(AppQueryVO query) {
        return appService.page(query);
    }

    @GetMapping("/list")
    @LoginRequired
    public Result<List<AppResponseVO>> listEnabled() {
        return Result.ok(appService.listEnabled());
    }

    @GetMapping("/{appId}/nodes")
    @LoginRequired
    public Result<List<ClientNodeVO>> getNodes(@PathVariable("appId") String appId) {
        return Result.ok(appService.getNodes(appId));
    }

    @GetMapping("/all-nodes")
    @LoginRequired
    public Result<List<ClientNodeVO>> getAllNodes() {
        return Result.ok(appService.getAllNodes());
    }

    @DeleteMapping("/{appId}/nodes/{hostId}")
    @LoginRequired
    public Result<Void> kickNode(@PathVariable("appId") String appId,
                                  @PathVariable("hostId") String hostId) {
        appService.kickNode(appId, hostId);
        return Result.ok(null);
    }
}
