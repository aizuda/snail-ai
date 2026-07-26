package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatRecordMetadataDTO;
import com.aizuda.snail.ai.feature.agent.service.AgentChatAttachmentService;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析并校验当前对话请求携带的附件。
 *
 * @author opensnail
 * @since 2026-07-07
 */
@Component
@Order(35)
@RequiredArgsConstructor
public class AttachmentResolveHandler implements AgentChatHandler {

    private final AgentChatAttachmentService attachmentService;
    private final AgentConversationRecordMapper recordMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated() || ctx.getRequestedAttachments() == null || ctx.getRequestedAttachments().isEmpty()) {
            return;
        }

        List<AgentChatAttachmentDTO> attachments = attachmentService.resolve(
                AgentChatAttachmentService.ResolveRequest.builder()
                        .attachments(ctx.getRequestedAttachments())
                        .user(ctx.getUser())
                        .modelConfig(ctx.getModelConfig())
                        .build());
        ctx.setAttachments(attachments);
        updateUserRecordMetadata(ctx, attachments);
    }

    private void updateUserRecordMetadata(AgentChatContext ctx, List<AgentChatAttachmentDTO> attachments) {
        if (recordMapper == null || ctx.getUserRecordId() == null || attachments == null || attachments.isEmpty()) {
            return;
        }
        AgentChatRecordMetadataDTO metadata = AgentChatRecordMetadataDTO.builder()
                .attachments(attachments.stream()
                        .map(this::toRecordAttachment)
                        .toList())
                .build();
        AgentConversationRecordPO record = new AgentConversationRecordPO();
        record.setId(ctx.getUserRecordId());
        record.setMetadata(JsonUtil.toJsonString(metadata));
        recordMapper.updateById(record);
    }

    private AgentChatRecordMetadataDTO.Attachment toRecordAttachment(AgentChatAttachmentDTO attachment) {
        return AgentChatRecordMetadataDTO.Attachment.builder()
                .resourceId(attachment.getResourceId())
                .type(attachment.getType())
                .fileName(attachment.getFileName())
                .mimeType(attachment.getMimeType())
                .size(attachment.getSize())
                .accessUrl(attachment.getAccessUrl())
                .build();
    }
}
