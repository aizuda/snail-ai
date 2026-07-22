package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.strategy.chunker.TokenAwareChunker;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds independent searchable chunks for document images.
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Component
@RequiredArgsConstructor
public class DocumentImageKnowledgeChunkBuilder {

    private static final int FIRST_PARAGRAPH_INDEX = 0;
    private static final int DEFAULT_START_CHUNK_INDEX = 0;
    private static final String LINE_SEPARATOR = "\n";
    private static final String TITLE_IMAGE_KNOWLEDGE = "图片知识";
    private static final String LABEL_DOCUMENT = "文档：";
    private static final String LABEL_PAGE = "页码：";
    private static final String LABEL_FIGURE_NO = "图号：";
    private static final String LABEL_FIGURE_TITLE = "图标题：";
    private static final String LABEL_SECTION = "章节：";
    private static final String LABEL_CAPTION = "图注：";
    private static final String LABEL_OCR = "OCR：";

    private final ImageOcrQualityEvaluator imageOcrQualityEvaluator;
    private final TokenAwareChunker tokenAwareChunker;

    public List<RagChunkPO> build(ImageKnowledgeChunkBuildRequest request) {
        if (request == null || request.getImages() == null || request.getImages().isEmpty()) {
            return List.of();
        }
        int nextChunkIndex = request.getStartChunkIndex() == null
                ? DEFAULT_START_CHUNK_INDEX
                : request.getStartChunkIndex();
        List<RagChunkPO> chunks = new ArrayList<>();
        for (DoclingParseResult.DoclingImage image : request.getImages()) {
            String content = buildContent(request, image);
            if (StrUtil.isBlank(content)) {
                continue;
            }
            chunks.add(RagChunkPO.builder()
                    .ragId(request.getRagId())
                    .documentId(request.getDocumentId())
                    .paragraphIndex(FIRST_PARAGRAPH_INDEX)
                    .chunkIndex(nextChunkIndex++)
                    .content(content)
                    .tokenCount(tokenAwareChunker.estimateTokens(content))
                    .contentHash(ContentHashUtil.sha256Hex(content))
                    .sourceType(RagChunkSourceTypeEnum.IMAGE.getValue())
                    .build());
        }
        return chunks;
    }

    private String buildContent(ImageKnowledgeChunkBuildRequest request, DoclingParseResult.DoclingImage image) {
        if (image == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        appendLine(lines, TITLE_IMAGE_KNOWLEDGE);
        appendLine(lines, LABEL_DOCUMENT, request.getDocumentName());
        appendLine(lines, LABEL_PAGE, image.getPage());
        appendLine(lines, LABEL_FIGURE_NO, image.getFigureNo());
        appendLine(lines, LABEL_FIGURE_TITLE, image.getFigureTitle());
        appendLine(lines, LABEL_SECTION, image.getSectionTitle());
        appendLine(lines, LABEL_CAPTION, image.getCaption());
        if (imageOcrQualityEvaluator.isIndexable(image.getOcrText())) {
            appendLine(lines, LABEL_OCR, image.getOcrText());
        }
        if (!hasSearchableImageKnowledge(image)) {
            return "";
        }
        return String.join(LINE_SEPARATOR, lines);
    }

    private boolean hasSearchableImageKnowledge(DoclingParseResult.DoclingImage image) {
        return StrUtil.isNotBlank(image.getFigureTitle())
                || StrUtil.isNotBlank(image.getSectionTitle())
                || StrUtil.isNotBlank(image.getCaption())
                || imageOcrQualityEvaluator.isIndexable(image.getOcrText());
    }

    private void appendLine(List<String> lines, String value) {
        if (StrUtil.isNotBlank(value)) {
            lines.add(value.trim());
        }
    }

    private void appendLine(List<String> lines, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (StrUtil.isBlank(text)) {
            return;
        }
        lines.add(label + text);
    }
}
