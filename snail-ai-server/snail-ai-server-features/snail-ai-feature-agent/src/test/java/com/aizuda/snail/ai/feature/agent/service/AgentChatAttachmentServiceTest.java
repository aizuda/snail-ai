package com.aizuda.snail.ai.feature.agent.service;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AgentChatAttachmentServiceTest {

    private static final Long RESOURCE_ID = 10L;
    private static final Long USER_ID = 7L;

    public void testShouldResolveImageAttachmentForVisionModel() {
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resource = imageResource("image/png", 4L, USER_ID);
        resourceService.content = "img".getBytes(StandardCharsets.UTF_8);
        AgentChatAttachmentService service = new AgentChatAttachmentService(resourceService);

        List<AgentChatAttachmentDTO> result = service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build());

        assertEquals(1, result.size());
        assertEquals(RESOURCE_ID, result.get(0).getResourceId());
        assertEquals("screen.png", result.get(0).getFileName());
        assertEquals("image/png", result.get(0).getMimeType());
        assertEquals("aW1n", result.get(0).getBase64());
    }

    public void testShouldRejectTooManyAttachments() {
        AgentChatAttachmentService service = new AgentChatAttachmentService(new FakeResourceService());
        List<AgentChatAttachmentDTO> attachments = List.of(
                imageAttachment(1L), imageAttachment(2L), imageAttachment(3L), imageAttachment(4L));

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(attachments)
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    public void testShouldRejectNonImageAttachmentType() {
        AgentChatAttachmentService service = new AgentChatAttachmentService(new FakeResourceService());
        AgentChatAttachmentDTO attachment = AgentChatAttachmentDTO.builder()
                .resourceId(RESOURCE_ID)
                .type("FILE")
                .build();

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(attachment))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    public void testShouldRejectImageWhenModelHasNoVisionCapability() {
        AgentChatAttachmentService service = new AgentChatAttachmentService(new FakeResourceService());

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(textModelConfig())
                .build()));
    }

    public void testShouldRejectMissingResource() {
        AgentChatAttachmentService service = new AgentChatAttachmentService(new FakeResourceService());

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    public void testShouldRejectResourceOwnedByAnotherUser() {
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resource = imageResource("image/png", 4L, 99L);
        AgentChatAttachmentService service = new AgentChatAttachmentService(resourceService);

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    public void testShouldRejectUnsupportedMimeType() {
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resource = imageResource("image/gif", 4L, USER_ID);
        AgentChatAttachmentService service = new AgentChatAttachmentService(resourceService);

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    public void testShouldRejectOversizedResource() {
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resource = imageResource("image/png", 10L * 1024L * 1024L + 1L, USER_ID);
        AgentChatAttachmentService service = new AgentChatAttachmentService(resourceService);

        assertThrows(SnailAiException.class, () -> service.resolve(AgentChatAttachmentService.ResolveRequest.builder()
                .attachments(List.of(imageAttachment(RESOURCE_ID)))
                .user(user(USER_ID))
                .modelConfig(visionModelConfig())
                .build()));
    }

    private static AgentChatAttachmentDTO imageAttachment(Long resourceId) {
        return AgentChatAttachmentDTO.builder()
                .resourceId(resourceId)
                .type("IMAGE")
                .build();
    }

    private static UserPO user(Long userId) {
        UserPO user = new UserPO();
        user.setId(userId);
        return user;
    }

    private static ChatDispatchRequest.ModelConfig visionModelConfig() {
        ConfigExtAttrsDTO attrs = new ConfigExtAttrsDTO();
        attrs.setCapabilities(List.of("vision"));
        return ChatDispatchRequest.ModelConfig.builder()
                .configJson(attrs)
                .build();
    }

    private static ChatDispatchRequest.ModelConfig textModelConfig() {
        ConfigExtAttrsDTO attrs = new ConfigExtAttrsDTO();
        attrs.setCapabilities(new ArrayList<>());
        return ChatDispatchRequest.ModelConfig.builder()
                .configJson(attrs)
                .build();
    }

    private static ResourcePO imageResource(String mimeType, Long size, Long creatorId) {
        return ResourcePO.builder()
                .id(RESOURCE_ID)
                .creatorId(creatorId)
                .originalName("screen.png")
                .mimeType(mimeType)
                .fileSize(size)
                .accessUrl("/files/abc")
                .build();
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Expected " + expectedType.getName() + " but got " + actual.getClass().getName());
        }
        throw new AssertionError("Expected " + expectedType.getName() + " but nothing was thrown");
    }

    private interface ThrowingRunnable {
        void run() throws Throwable;
    }

    private static class FakeResourceService extends ResourceService {

        private ResourcePO resource;
        private byte[] content = new byte[0];

        private FakeResourceService() {
            super(null, null);
        }

        @Override
        public ResourcePO getById(Long resourceId) {
            return resource;
        }

        @Override
        public InputStream load(Long resourceId) {
            return new ByteArrayInputStream(content);
        }
    }
}
