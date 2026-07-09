package com.aizuda.snail.ai.openapi.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceUploadRequest;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * OpenAPI 资源服务。
 *
 * @author opensnail
 * @since 2026-07-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiResourceService {

    private final OpenApiUserResolver openApiUserResolver;
    private final ResourceService resourceService;

    public OpenApiResourceResponse upload(OpenApiResourceUploadRequest request) {
        String appId = OpenApiSessionUtils.current().getAppId();
        UserPO requestUser = openApiUserResolver.resolvePlatformUser(appId, request.getOpenId());
        String bizType = StrUtil.blankToDefault(request.getBizType(), ResourceBizTypeEnum.CHAT_IMAGE.getValue());
        ResourcePO resource = resourceService.upload(
                new ByteArrayInputStream(request.getContent()),
                request.getOriginalName(),
                request.getFileSize(),
                bizType,
                request.getBizId(),
                requestUser.getId());
        log.info("OpenAPI upload resource success id={} bizType={} creatorId={}",
                resource.getId(), bizType, requestUser.getId());
        return toResponse(resource);
    }

    private OpenApiResourceResponse toResponse(ResourcePO resource) {
        return OpenApiResourceResponse.builder()
                .id(resource.getId())
                .storageKey(resource.getStorageKey())
                .originalName(resource.getOriginalName())
                .fileSize(resource.getFileSize())
                .mimeType(resource.getMimeType())
                .storageType(resource.getStorageType())
                .accessUrl(resource.getAccessUrl())
                .bizType(resource.getBizType())
                .bizId(resource.getBizId())
                .creatorId(resource.getCreatorId())
                .createDt(resource.getCreateDt())
                .build();
    }
}
