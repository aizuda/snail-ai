package com.aizuda.snail.ai.agent.core.executor.prompt;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 默认 Prompt 工厂。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class DefaultPromptFactory implements PromptFactory {

    private static final String ASSISTANT_ROLE = "assistant";

    @Override
    public Prompt build(ChatDispatchRequest request) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";
        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (request.getHistoryMessages() != null) {
            request.getHistoryMessages().stream()
                    .filter(history -> history.getContent() != null && !history.getContent().isEmpty())
                    .map(this::toMessage)
                    .forEach(messages::add);
        }
        if (request.getUserMessage() != null && !request.getUserMessage().isEmpty()) {
            messages.add(buildUserMessage(request));
        }
        return new Prompt(messages);
    }

    private UserMessage buildUserMessage(ChatDispatchRequest request) {
        List<Media> media = toMedia(request.getMediaAttachments());
        if (media.isEmpty()) {
            return new UserMessage(request.getUserMessage());
        }
        return UserMessage.builder()
                .text(request.getUserMessage())
                .media(media)
                .build();
    }

    private List<Media> toMedia(List<ChatDispatchRequest.MediaAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .filter(attachment -> attachment.getMimeType() != null
                        && attachment.getBase64() != null
                        && !attachment.getBase64().isEmpty())
                .map(attachment -> Media.builder()
                        .mimeType(MimeTypeUtils.parseMimeType(attachment.getMimeType()))
                        .data(Base64.getDecoder().decode(attachment.getBase64()))
                        .name(attachment.getFileName())
                        .build())
                .toList();
    }

    private Message toMessage(ChatDispatchRequest.HistoryMessage history) {
        if (ASSISTANT_ROLE.equals(history.getRole())) {
            return new AssistantMessage(history.getContent());
        }
        return new UserMessage(history.getContent());
    }
}
