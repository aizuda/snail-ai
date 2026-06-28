package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.service.app.AppService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.app.*;
import com.aizuda.snail.ai.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用（App）管理接口：应用 CRUD、启用状态切换与接入节点管理。
 * <p>
 * 全部端点均需 ADMIN 角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    /** 创建应用（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<AppResponseVO> create(@RequestBody AppRequestVO request) {
        return Result.ok(appService.create(request));
    }

    /** 更新应用（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<AppResponseVO> update(@PathVariable("id") Long id,
                                         @RequestBody AppRequestVO request) {
        return Result.ok(appService.update(id, request));
    }

    /** 删除应用（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        appService.delete(id);
        return Result.ok(null);
    }

    /** 切换应用启用 / 停用状态（管理员）。 */
    @PostMapping("/{id}/toggle-status")
    @LoginRequired
    public Result<Void> toggleStatus(@PathVariable("id") Long id) {
        appService.toggleStatus(id);
        return Result.ok(null);
    }

    /** 分页查询应用列表（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<AppResponseVO>> page(AppQueryVO query) {
        return appService.page(query);
    }

    /** 查询已启用的应用列表（管理员）。 */
    @GetMapping("/list")
    @LoginRequired
    public Result<List<AppResponseVO>> listEnabled() {
        return Result.ok(appService.listEnabled());
    }

    /** 查询指定应用下的在线接入节点（管理员）。 */
    @GetMapping("/{appId}/nodes")
    @LoginRequired
    public Result<List<ClientNodeVO>> getNodes(@PathVariable("appId") String appId) {
        return Result.ok(appService.getNodes(appId));
    }

    /** 查询全部应用的在线接入节点（管理员）。 */
    @GetMapping("/all-nodes")
    @LoginRequired
    public Result<List<ClientNodeVO>> getAllNodes() {
        return Result.ok(appService.getAllNodes());
    }

    /** 踢除指定接入节点（管理员）。 */
    @DeleteMapping("/{appId}/nodes/{hostId}")
    @LoginRequired
    public Result<Void> kickNode(@PathVariable("appId") String appId,
                                  @PathVariable("hostId") String hostId) {
        appService.kickNode(appId, hostId);
        return Result.ok(null);
    }
}
