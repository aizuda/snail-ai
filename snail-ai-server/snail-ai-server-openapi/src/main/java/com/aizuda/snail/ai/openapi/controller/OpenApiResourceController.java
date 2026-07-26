package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceUploadRequest;
import com.aizuda.snail.ai.openapi.service.OpenApiResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI 资源接口。
 *
 * @author opensnail
 * @since 2026-07-09
 */
@RestController
@RequiredArgsConstructor
public class OpenApiResourceController {

    private final OpenApiResourceService openApiResourceService;

    @PostMapping(OpenApiPathConstants.OPEN_API_RESOURCE_UPLOAD)
    public Result<OpenApiResourceResponse> upload(@RequestBody @Validated OpenApiResourceUploadRequest request) {
        return Result.ok(openApiResourceService.upload(request));
    }
}
