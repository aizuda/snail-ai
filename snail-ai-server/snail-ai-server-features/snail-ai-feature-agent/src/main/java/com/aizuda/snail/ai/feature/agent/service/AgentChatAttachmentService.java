package com.aizuda.snail.ai.feature.agent.service;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;
import com.aizuda.snail.ai.feature.agent.enums.AgentChatAttachmentTypeEnum;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 解析智能体对话图片附件：校验数量、类型、归属、格式与大小，并将资源内容读取为 base64 供多模态模型使用。
 * 任一校验不通过即抛出 {@link SnailAiException} 终止本次对话请求。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-07-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatAttachmentService {

    private static final int MAX_IMAGE_COUNT = 3;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * BYTES_PER_MEGABYTE;
    private static final String VISION_CAPABILITY = "vision";
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final ResourceService resourceService;

    /**
     * 解析并校验本次对话携带的全部附件
     *
     * @param request 解析请求（附件列表、当前用户、目标模型配置）
     * @return 解析完成（含 base64 内容）的附件列表；无附件时返回空列表
     */
    public List<AgentChatAttachmentDTO> resolve(ResolveRequest request) {
        List<AgentChatAttachmentDTO> attachments = request == null ? List.of() : request.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        validateAttachmentCount(attachments);
        validateVisionModel(request.getModelConfig());

        List<AgentChatAttachmentDTO> resolved = attachments.stream()
                .map(attachment -> resolveAttachment(attachment, request.getUser()))
                .toList();
        log.debug("对话附件解析完成, count={}, totalBytes={}",
                resolved.size(),
                resolved.stream().mapToLong(a -> a.getSize() != null ? a.getSize() : 0L).sum());
        return resolved;
    }

    private void validateAttachmentCount(List<AgentChatAttachmentDTO> attachments) {
        if (attachments.size() > MAX_IMAGE_COUNT) {
            throw new SnailAiException("每次对话最多上传 {} 张图片", MAX_IMAGE_COUNT);
        }
    }

    /** 校验目标模型具备 vision 能力，否则图片将被模型当作无效输入 */
    private void validateVisionModel(ChatDispatchRequest.ModelConfig modelConfig) {
        if (modelConfig == null || !hasVisionCapability(modelConfig.getConfigJson())) {
            log.warn("图片对话被拒绝：模型不具备 vision 能力, modelKey={}",
                    modelConfig != null ? modelConfig.getModelKey() : null);
            throw new SnailAiException("当前模型不支持图片对话，请切换视觉模型");
        }
    }

    private boolean hasVisionCapability(ConfigExtAttrsDTO configJson) {
        if (configJson == null || configJson.getCapabilities() == null) {
            return false;
        }
        return configJson.getCapabilities().stream().anyMatch(VISION_CAPABILITY::equalsIgnoreCase);
    }

    private AgentChatAttachmentDTO resolveAttachment(AgentChatAttachmentDTO attachment, UserPO user) {
        validateAttachmentType(attachment);
        ResourcePO resource = requireResource(attachment.getResourceId());
        validateOwner(resource, user);
        validateMimeType(resource);
        validateSize(resource);

        return AgentChatAttachmentDTO.builder()
                .resourceId(resource.getId())
                .type(AgentChatAttachmentTypeEnum.IMAGE.getValue())
                .fileName(resource.getOriginalName())
                .mimeType(resource.getMimeType())
                .size(resource.getFileSize())
                .accessUrl(resource.getAccessUrl())
                .base64(loadBase64(resource.getId()))
                .build();
    }

    private void validateAttachmentType(AgentChatAttachmentDTO attachment) {
        if (attachment == null || !AgentChatAttachmentTypeEnum.IMAGE.matches(attachment.getType())) {
            throw new SnailAiException("仅支持图片附件");
        }
        if (attachment.getResourceId() == null) {
            throw new SnailAiException("图片资源不能为空");
        }
    }

    private ResourcePO requireResource(Long resourceId) {
        ResourcePO resource = resourceService.getById(resourceId);
        if (resource == null) {
            throw new SnailAiException("图片资源不存在");
        }
        return resource;
    }

    private void validateOwner(ResourcePO resource, UserPO user) {
        if (user == null || user.getId() == null || !user.getId().equals(resource.getCreatorId())) {
            throw new SnailAiException("无权使用该图片资源");
        }
    }

    private void validateMimeType(ResourcePO resource) {
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(resource.getMimeType())) {
            throw new SnailAiException("仅支持 JPG、PNG、WEBP 图片");
        }
    }

    private void validateSize(ResourcePO resource) {
        Long fileSize = resource.getFileSize();
        if (fileSize == null || fileSize > MAX_IMAGE_SIZE_BYTES) {
            throw new SnailAiException("单张图片不能超过 10MB");
        }
    }

    private String loadBase64(Long resourceId) {
        try (InputStream inputStream = resourceService.load(resourceId)) {
            return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new SnailAiException("读取图片资源失败", e);
        }
    }

    @Data
    @Builder
    public static class ResolveRequest {
        private List<AgentChatAttachmentDTO> attachments;
        private UserPO user;
        private ChatDispatchRequest.ModelConfig modelConfig;
    }
}
