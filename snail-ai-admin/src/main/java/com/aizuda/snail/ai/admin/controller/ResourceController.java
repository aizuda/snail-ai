package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.security.annotation.OriginalControllerReturnValue;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.admin.service.resource.ResourceAdminService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.resource.ResourceQueryVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceResponseVO;
import com.aizuda.snail.ai.admin.vo.resource.ResourceUploadRequestVO;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceAdminService resourceAdminService;
    private final ResourceService resourceService;

    @PostMapping("/upload")
    @LoginRequired
    public Result<ResourceResponseVO> upload(@RequestParam("file") MultipartFile file,
                                              ResourceUploadRequestVO request) {
        return Result.ok(resourceAdminService.upload(file, request));
    }

    @GetMapping("/{id}/preview")
    @LoginRequired(role = RoleEnum.USER)
    @OriginalControllerReturnValue
    public ResponseEntity<InputStreamResource> preview(@PathVariable("id") Long id) {
        ResourcePO resource = resourceService.getById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream is = resourceService.load(id);
        String encodedName = URLEncoder.encode(resource.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MediaType mediaType = resource.getMimeType() != null
                ? MediaType.parseMediaType(resource.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/{id}/download")
    @LoginRequired(role = RoleEnum.USER)
    @OriginalControllerReturnValue
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) {
        ResourcePO resource = resourceService.getById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream is = resourceService.load(id);
        String encodedName = URLEncoder.encode(resource.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(is));
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
