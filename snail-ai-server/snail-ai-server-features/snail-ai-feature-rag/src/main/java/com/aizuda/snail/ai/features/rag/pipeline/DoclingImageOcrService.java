package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private final ModelRuntimeHandler modelRuntimeHandler;

    /**
     * 对 base64 图片做 OCR 文字识别。
     * <p>
     * 通过 chat 模型直接传入包含图片 base64 的文本 prompt（data URI 格式），
     * 由模型自行解析图片内容。要求模型支持视觉输入。
     *
     * @param base64Image      图片 base64 内容（不含 data:image 前缀）
     * @param chatModelConfigId 对话模型配置 ID（需支持视觉输入）
     * @return 识别出的文字，失败时返回空字符串
     */
    public String recognize(String base64Image, Long chatModelConfigId) {
        if (StrUtil.isBlank(base64Image) || chatModelConfigId == null) {
            return "";
        }
        try {
            // 将 base64 图片以 data URI 格式嵌入 prompt，视觉模型会自动识别
            String userPrompt = "请识别这张图片中的所有文字。\n![image](data:image/png;base64," + base64Image + ")";

            String result = modelRuntimeHandler.chat(
                    new ModelRuntimeHandler.ChatRequest(chatModelConfigId, userPrompt, OCR_SYSTEM_PROMPT));

            if (result == null) {
                return "";
            }
            String trimmed = result.trim();
            if (trimmed.isEmpty() || "无".equals(trimmed) || "没有文字".equals(trimmed)
                    || "图片中没有文字".equals(trimmed) || "No text found.".equalsIgnoreCase(trimmed)) {
                return "";
            }
            log.debug("OCR 识别成功 modelConfigId={} textLen={}", chatModelConfigId, trimmed.length());
            return trimmed;
        } catch (Exception e) {
            log.warn("OCR 识别失败 modelConfigId={}: {}", chatModelConfigId, e.getMessage());
            return "";
        }
    }
}
