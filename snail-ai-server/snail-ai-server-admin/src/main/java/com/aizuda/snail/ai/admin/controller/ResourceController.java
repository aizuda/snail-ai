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

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceAdminService resourceAdminService;

    @PostMapping("/upload")
    @LoginRequired
    public Result<ResourceResponseVO> upload(@RequestParam("file") MultipartFile file,
                                              ResourceUploadRequestVO request) {
        return Result.ok(resourceAdminService.upload(file, request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        resourceAdminService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<ResourceResponseVO>> page(ResourceQueryVO query) {
        return resourceAdminService.page(query);
    }
}
