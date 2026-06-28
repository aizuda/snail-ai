package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.service.resource.ResourceAdminService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.resource.ResourceQueryVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceResponseVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceUploadRequestVO;
import com.aizuda.snail.ai.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资源管理接口：文件资源上传、删除与分页查询（后台管理用）。
 * <p>
 * 全部端点均需 ADMIN 角色；公开访问入口见 {@link FileAccessController}。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceAdminService resourceAdminService;

    /** 上传资源文件（管理员）。 */
    @PostMapping("/upload")
    @LoginRequired
    public Result<ResourceResponseVO> upload(@RequestParam("file") MultipartFile file,
                                              ResourceUploadRequestVO request) {
        return Result.ok(resourceAdminService.upload(file, request));
    }

    /** 删除资源（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        resourceAdminService.delete(id);
        return Result.ok(null);
    }

    /** 分页查询资源列表（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<ResourceResponseVO>> page(ResourceQueryVO query) {
        return resourceAdminService.page(query);
    }
}
