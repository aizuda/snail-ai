package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.Base64;
import java.util.List;

/**
 * Docling 图片 OCR 服务——调用视觉语言模型识别图片中的文字。
 *
 * @author opensnail
 * @since 2026-07-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoclingImageOcrService {

    private static final String OCR_SYSTEM_PROMPT = "你是一个 OCR 识别助手。请识别图片中的所有文字，只输出识别结果，不要添加任何解释。如果没有文字，输出空字符串。";
    private static final MimeType MIME_PNG = MimeType.valueOf("image/png");

    private final ModelRuntimeHandler modelRuntimeHandler;
    private final ModelConfigHandler modelConfigHandler;

    /**
     * 对 base64 图片做 OCR 文字识别。
     *
     * @param base64Image     图片 base64 内容（不含 data:image 前缀）
     * @param chatModelConfigId 对话模型配置 ID（需支持视觉输入）
     * @return 识别出的文字，失败时返回空字符串
     */
    public String recognize(String base64Image, Long chatModelConfigId) {
        if (StrUtil.isBlank(base64Image) || chatModelConfigId == null) {
            return "";
        }
        try {
            ModelConfigInfoDTO config = modelConfigHandler.getConfigInfo(chatModelConfigId);
            if (config == null) {
                log.warn("OCR 模型配置不存在: modelConfigId={}", chatModelConfigId);
                return "";
            }

            // 构建多模态消息：图片 + OCR 提示词
            Media media = Media.builder()
                    .id("docling-image")
                    .mimeType(MIME_PNG)
                    .data(Base64.getDecoder().decode(base64Image))
                    .build();

            UserMessage userMessage = new UserMessage(OCR_SYSTEM_PROMPT, List.of(media));

            // 使用 ModelRuntimeHandler 的 chat 方法（纯文本）不支持 media，
            // 直接构建 ChatClient 发送多模态请求
            ChatClient chatClient = modelRuntimeHandler.buildChatClient(config);
            String result = chatClient.prompt(new Prompt(List.of(new SystemMessage(OCR_SYSTEM_PROMPT), userMessage)))
                    .call()
                    .content();

            if (result == null) {
                return "";
            }
            String trimmed = result.trim();
            // 过滤掉模型可能返回的"图片中没有文字"之类的废话
            if ("图片中没有文字".equals(trimmed) || "没有文字".equals(trimmed)
                    || "无".equals(trimmed) || "No text found.".equalsIgnoreCase(trimmed)) {
                return "";
            }
            return trimmed;
        } catch (Exception e) {
            log.warn("OCR 识别失败: {}", e.getMessage());
            return "";
        }
    }
}
