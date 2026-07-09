package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceUploadRequest;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.features.resource.strategy.ResourceStorageFactory;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiResourceServiceTest {

    @AfterEach
    void tearDown() {
        OpenApiSessionUtils.clear();
    }

    @Test
    void uploadShouldStoreChatImageForResolvedPlatformUser() {
        OpenApiSessionUtils.set(OpenApiSessionUtils.OpenApiSession.builder()
                .appId("app-1")
                .build());
        CapturingResourceService resourceService = new CapturingResourceService();
        OpenApiResourceService service = new OpenApiResourceService(new FakeOpenApiUserResolver(), resourceService);
        OpenApiResourceUploadRequest request = new OpenApiResourceUploadRequest();
        request.setOpenId("open-1");
        request.setOriginalName("image.png");
        request.setFileSize(3L);
        request.setBizId(9L);
        request.setContent(new byte[] {1, 2, 3});

        service.upload(request);

        assertEquals("image.png", resourceService.originalName);
        assertEquals(3L, resourceService.fileSize);
        assertEquals(ResourceBizTypeEnum.CHAT_IMAGE.getValue(), resourceService.bizType);
        assertEquals(9L, resourceService.bizId);
        assertEquals(7L, resourceService.creatorId);
    }

    private static class FakeOpenApiUserResolver extends OpenApiUserResolver {

        private FakeOpenApiUserResolver() {
            super(null, null);
        }

        @Override
        public UserPO resolvePlatformUser(String appId, String openId) {
            UserPO user = new UserPO();
            user.setId(7L);
            return user;
        }
    }

    private static class CapturingResourceService extends ResourceService {

        private String originalName;
        private long fileSize;
        private String bizType;
        private Long bizId;
        private Long creatorId;

        private CapturingResourceService() {
            super((ResourceMapper) null, (ResourceStorageFactory) null);
        }

        @Override
        public ResourcePO upload(InputStream inputStream, String originalName, long fileSize,
                                 String bizType, Long bizId, Long creatorId) {
            this.originalName = originalName;
            this.fileSize = fileSize;
            this.bizType = bizType;
            this.bizId = bizId;
            this.creatorId = creatorId;

            ResourcePO resource = new ResourcePO();
            resource.setId(15L);
            resource.setOriginalName(originalName);
            resource.setFileSize(fileSize);
            resource.setBizType(bizType);
            resource.setBizId(bizId);
            resource.setCreatorId(creatorId);
            return resource;
        }
    }
}
