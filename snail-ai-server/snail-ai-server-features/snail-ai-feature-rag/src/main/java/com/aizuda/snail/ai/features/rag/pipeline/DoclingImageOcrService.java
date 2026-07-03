package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;

/**
 * Docling 图片 OCR 服务——调用视觉语言模型识别图片中的文字。
 *
 * @author opensnail
 * @since 2026-07-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoclingImageOcrService implements ImageOcrHandler {

    private static final String OCR_SYSTEM_PROMPT = "你是一个 OCR 识别助手。请识别图片中的所有文字，只输出识别结果，不要添加任何解释。如果没有文字，输出空字符串。";
    private static final String OCR_USER_PROMPT = "请识别这张图片中的所有文字。";
    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
    private static final String DEFAULT_IMAGE_FILE_NAME = "docling-ocr.png";
    private static final String EMPTY_TEXT_CN = "无";
    private static final String NO_TEXT_CN = "没有文字";
    private static final String IMAGE_NO_TEXT_CN = "图片中没有文字";
    private static final String NO_TEXT_EN = "No text found.";
    private static final String HTTP_TOO_MANY_REQUESTS = "429";
    private static final String QUOTA_KEYWORD = "quota";
    private static final String TOKEN_LIMIT_KEYWORD = "token-limit";
    private static final String RATE_LIMIT_KEYWORD = "rate limit";
    private static final String QUOTA_CN_KEYWORD = "额度";
    private static final String RATE_LIMIT_CN_KEYWORD = "限流";

    private final ModelRuntimeHandler modelRuntimeHandler;

    @Override
    public String recognize(ImageOcrRequest request) {
        if (request == null) {
            return "";
        }
        return recognize(request.getBase64Image(), request.getModelConfigId());
    }

    /**
     * 对 base64 图片做 OCR 文字识别。
     * <p>
     * 通过 Spring AI media 消息传入图片，避免将 base64 内容作为文本 token 发送。
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
            String result = modelRuntimeHandler.chat(
                    new ModelRuntimeHandler.ChatMediaRequest(
                            chatModelConfigId,
                            OCR_USER_PROMPT,
                            OCR_SYSTEM_PROMPT,
                            DEFAULT_IMAGE_MIME_TYPE,
                            DEFAULT_IMAGE_FILE_NAME,
                            Base64.getMimeDecoder().decode(base64Image)));

            if (result == null) {
                return "";
            }
            String trimmed = result.trim();
            if (isEmptyOcrResult(trimmed)) {
                return "";
            }
            log.debug("OCR 识别成功 modelConfigId={} textLen={}", chatModelConfigId, trimmed.length());
            return trimmed;
        } catch (ModelCallException e) {
            if (isQuotaExceeded(e)) {
                throw new DoclingImageOcrQuotaExceededException("OCR 模型配额不足或触发限流", e);
            }
            log.warn("OCR 识别失败 modelConfigId={}: {}", chatModelConfigId, e.getMessage());
            return "";
        } catch (Exception e) {
            if (isQuotaExceeded(e)) {
                throw new DoclingImageOcrQuotaExceededException("OCR 模型配额不足或触发限流", e);
            }
            log.warn("OCR 识别失败 modelConfigId={}: {}", chatModelConfigId, e.getMessage());
            return "";
        }
    }

    private boolean isEmptyOcrResult(String result) {
        return result.isEmpty()
                || EMPTY_TEXT_CN.equals(result)
                || NO_TEXT_CN.equals(result)
                || IMAGE_NO_TEXT_CN.equals(result)
                || NO_TEXT_EN.equalsIgnoreCase(result);
    }

    private boolean isQuotaExceeded(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (containsQuotaSignal(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsQuotaSignal(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return lowerMessage.contains(HTTP_TOO_MANY_REQUESTS)
                || lowerMessage.contains(QUOTA_KEYWORD)
                || lowerMessage.contains(TOKEN_LIMIT_KEYWORD)
                || lowerMessage.contains(RATE_LIMIT_KEYWORD)
                || message.contains(QUOTA_CN_KEYWORD)
                || message.contains(RATE_LIMIT_CN_KEYWORD);
    }
}
