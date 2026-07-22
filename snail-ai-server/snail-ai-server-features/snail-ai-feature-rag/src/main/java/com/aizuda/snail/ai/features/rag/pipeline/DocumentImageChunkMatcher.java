package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Associates extracted document images with generated text chunks.
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Component
public class DocumentImageChunkMatcher {

    private static final int MIN_CAPTION_BODY_LENGTH = 2;
    private static final int MIN_OCR_MATCH_LENGTH = 6;
    private static final Pattern FIGURE_NO_PREFIX_PATTERN = Pattern.compile(
            "^(?:图|Figure)\\s*[0-9一二三四五六七八九十百千万]+\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_TEXT_PATTERN = Pattern.compile("[\\p{Punct}\\s，。！？、：；（）【】《》]+");

    public Long matchChunkId(DoclingParseResult.DoclingImage image, List<RagChunkPO> chunks) {
        if (image == null || chunks == null || chunks.isEmpty()) {
            return null;
        }
        String caption = StrUtil.trimToEmpty(image.getCaption());
        String figureTitle = StrUtil.trimToEmpty(image.getFigureTitle());
        String sectionTitle = StrUtil.trimToEmpty(image.getSectionTitle());
        String ocrText = StrUtil.trimToEmpty(image.getOcrText());
        if (StrUtil.isBlank(caption)
                && StrUtil.isBlank(figureTitle)
                && StrUtil.isBlank(sectionTitle)
                && StrUtil.isBlank(ocrText)) {
            return null;
        }
        for (RagChunkPO chunk : chunks) {
            if (chunk == null || StrUtil.isBlank(chunk.getContent())) {
                continue;
            }
            if (matches(chunk.getContent(), caption, figureTitle, sectionTitle, ocrText)) {
                return chunk.getId();
            }
        }
        return null;
    }

    private boolean matches(String content, String caption, String figureTitle, String sectionTitle, String ocrText) {
        if (isMeaningfulCaption(caption) && content.contains(caption)) {
            return true;
        }
        if (isMeaningfulText(figureTitle, MIN_CAPTION_BODY_LENGTH) && content.contains(figureTitle)) {
            return true;
        }
        if (isMeaningfulText(sectionTitle, MIN_CAPTION_BODY_LENGTH) && content.contains(sectionTitle)) {
            return true;
        }
        return isMeaningfulOcrText(ocrText) && content.contains(ocrText);
    }

    private boolean isMeaningfulCaption(String caption) {
        if (StrUtil.isBlank(caption)) {
            return false;
        }
        String body = FIGURE_NO_PREFIX_PATTERN.matcher(caption.trim()).replaceFirst("");
        return normalizedLength(body) >= MIN_CAPTION_BODY_LENGTH;
    }

    private boolean isMeaningfulOcrText(String ocrText) {
        return normalizedLength(ocrText) >= MIN_OCR_MATCH_LENGTH;
    }

    private boolean isMeaningfulText(String text, int minLength) {
        return normalizedLength(text) >= minLength;
    }

    private int normalizedLength(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        return NON_TEXT_PATTERN.matcher(text).replaceAll("").length();
    }
}
