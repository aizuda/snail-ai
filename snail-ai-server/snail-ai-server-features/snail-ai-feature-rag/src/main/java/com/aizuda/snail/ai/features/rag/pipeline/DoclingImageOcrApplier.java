package com.aizuda.snail.ai.features.rag.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.aizuda.snail.ai.features.rag.docling.DoclingResponseParser;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParseResult;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.VisionModelConfigResolver;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;

/**
 * 将 Docling 图片 OCR 文本合并回解析结果，确保分块、向量化和全文搜索前已经包含图片文字。
 * <p>
 * 识别顺序：优先复用解析结果中已有的图片文字，其次批量走 PaddleOCR，仍无结果且开启回退时
 * 逐张调用视觉大模型 OCR（配额/限流耗尽后跳过剩余图片）；最后把 OCR 文本合并进对应元素并重建 Markdown。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-07-03
 */
@Slf4j
@Component
public class DoclingImageOcrApplier {

    private static final String TYPE_PICTURE = "picture";
    private static final String IMAGE_PLACEHOLDER = "[image]";
    private static final String TEXT_SEPARATOR = "\n";

    private final DoclingResponseParser responseParser;
    private final DoclingProperties doclingProperties;
    private final ImageOcrHandler paddleImageOcrHandler;
    private final ImageOcrHandler visionImageOcrHandler;
    private final VisionModelConfigResolver visionModelConfigResolver;
    private final ImageOcrQualityEvaluator imageOcrQualityEvaluator;

    public DoclingImageOcrApplier(DoclingResponseParser responseParser,
                                  DoclingProperties doclingProperties,
                                  @Qualifier("paddleImageOcrHandler") ImageOcrHandler paddleImageOcrHandler,
                                  @Qualifier("doclingImageOcrService") ImageOcrHandler visionImageOcrHandler,
                                  VisionModelConfigResolver visionModelConfigResolver,
                                  ImageOcrQualityEvaluator imageOcrQualityEvaluator) {
        this.responseParser = responseParser;
        this.doclingProperties = doclingProperties;
        this.paddleImageOcrHandler = paddleImageOcrHandler;
        this.visionImageOcrHandler = visionImageOcrHandler;
        this.visionModelConfigResolver = visionModelConfigResolver;
        this.imageOcrQualityEvaluator = imageOcrQualityEvaluator;
    }

    /**
     * 对解析结果中的图片执行 OCR 并回写内容
     *
     * @param config      知识库配置（决定是否启用图片 OCR 及回退模型）
     * @param document    文档记录（仅用于日志上下文）
     * @param parseResult 文档解析结果，OCR 文本合并后其 content/markdownContent 会被重建
     */
    void apply(RagConfigDO config, RagDocumentPO document, DocumentParseResult parseResult) {
        if (!isImageOcrEnabled(config)) {
            return;
        }
        if (parseResult == null || parseResult.getDoclingResult() == null) {
            return;
        }
        DoclingParseResult doclingResult = parseResult.getDoclingResult();
        List<DoclingParseResult.DoclingImage> images = doclingResult.getImages();
        if (images == null || images.isEmpty()) {
            return;
        }
        Long documentId = document == null ? null : document.getId();
        log.info("Docling image OCR started. documentId={} imageCount={} paddleEnabled={} visionFallbackEnabled={}",
                documentId, images.size(), doclingProperties != null && doclingProperties.isPaddleOcrEnabled(),
                isVisionOcrFallbackEnabled());
        List<ImageOcrRequest> paddleRequests = new ArrayList<>();
        Map<Integer, DoclingParseResult.DoclingImage> paddleImageByIndex = new LinkedHashMap<>();
        for (int i = 0; i < images.size(); i++) {
            int imageIndex = i + 1;
            DoclingParseResult.DoclingImage image = images.get(i);
            if (image == null || StrUtil.isBlank(image.getBase64())) {
                continue;
            }
            if (StrUtil.isNotBlank(image.getOcrText())) {
                continue;
            }
            String ocrText = resolveExistingPictureText(doclingResult, image);
            if (StrUtil.isNotBlank(ocrText)) {
                image.setOcrText(ocrText);
                continue;
            }
            ImageOcrRequest request = buildImageOcrRequest(documentId, image, imageIndex, null);
            paddleRequests.add(request);
            paddleImageByIndex.put(imageIndex, image);
        }
        applyPaddleBatchOcr(documentId, paddleRequests, paddleImageByIndex);

        Long visionModelConfigId = isVisionOcrFallbackEnabled() ? resolveVisionModelConfigId(config) : null;
        boolean skipRemainingVisionOcr = false;
        for (int i = 0; i < images.size(); i++) {
            int imageIndex = i + 1;
            DoclingParseResult.DoclingImage image = images.get(i);
            if (image == null || StrUtil.isBlank(image.getBase64()) || StrUtil.isNotBlank(image.getOcrText())) {
                continue;
            }
            if (!shouldUseVisionFallback(visionModelConfigId, skipRemainingVisionOcr)) {
                continue;
            }
            try {
                String ocrText = visionImageOcrHandler.recognize(
                        buildImageOcrRequest(documentId, image, imageIndex, visionModelConfigId));
                if (StrUtil.isNotBlank(ocrText)) {
                    image.setOcrText(ocrText);
                }
            } catch (DoclingImageOcrQuotaExceededException e) {
                skipRemainingVisionOcr = true;
                log.warn("Docling vision OCR fallback stopped because model quota or rate limit was reached. "
                                + "documentId={} imageIndex={} modelConfigId={}",
                        documentId, imageIndex, visionModelConfigId);
            }
        }
        log.info("Docling image OCR finished. documentId={} textImages={} totalImages={}",
                documentId, countImagesWithOcrText(images), images.size());
        if (!hasIndexableOcrText(images)) {
            return;
        }
        mergeOcrTextIntoElements(doclingResult);
        String cleanMarkdown = responseParser.toCleanMarkdown(doclingResult);
        parseResult.setContent(cleanMarkdown);
        parseResult.setMarkdownContent(cleanMarkdown);
    }

    private boolean isImageOcrEnabled(RagConfigDO config) {
        return config != null
                && config.getChunkParams() != null
                && Boolean.TRUE.equals(config.getChunkParams().getImageOcr());
    }

    private ImageOcrRequest buildImageOcrRequest(Long documentId, DoclingParseResult.DoclingImage image,
                                                 int imageIndex, Long modelConfigId) {
        return ImageOcrRequest.builder()
                .documentId(documentId)
                .imageIndex(imageIndex)
                .base64Image(image.getBase64())
                .modelConfigId(modelConfigId)
                .build();
    }

    /** 批量提交 PaddleOCR 识别并按 imageIndex 回填结果 */
    private void applyPaddleBatchOcr(Long documentId, List<ImageOcrRequest> requests,
                                     Map<Integer, DoclingParseResult.DoclingImage> imageByIndex) {
        if (requests.isEmpty()) {
            log.info("Docling image OCR skipped PaddleOCR because no image requires recognition. documentId={}", documentId);
            return;
        }
        log.info("Docling PaddleOCR batch started. documentId={} count={}", documentId, requests.size());
        List<PaddleImageOcrResult> results = paddleImageOcrHandler.recognizeBatch(requests);
        int successCount = 0;
        int emptyCount = 0;
        int failedCount = 0;
        for (PaddleImageOcrResult result : results) {
            if (result == null) {
                failedCount++;
                continue;
            }
            if (result.getStatus() == PaddleImageOcrResult.Status.SUCCESS) {
                DoclingParseResult.DoclingImage image = imageByIndex.get(result.getImageIndex());
                if (image != null && StrUtil.isNotBlank(result.getText())) {
                    image.setOcrText(result.getText());
                    successCount++;
                    continue;
                }
            }
            if (result.getStatus() == PaddleImageOcrResult.Status.EMPTY) {
                emptyCount++;
                continue;
            }
            failedCount++;
            log.warn("Docling PaddleOCR image failed. documentId={} imageIndex={} error={}",
                    documentId, result.getImageIndex(), result.getError());
        }
        log.info("Docling PaddleOCR batch finished. documentId={} count={} success={} empty={} failed={}",
                documentId, requests.size(), successCount, emptyCount, failedCount);
    }

    /** 复用解析结果中已有的图片文字（如 Docling 自带 OCR 注解），避免重复识别浪费额度 */
    private String resolveExistingPictureText(DoclingParseResult result, DoclingParseResult.DoclingImage image) {
        List<DoclingParseResult.DoclingElement> elements = result.getElements();
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        for (DoclingParseResult.DoclingElement element : elements) {
            if (!isPictureElementForImage(element, image)) {
                continue;
            }
            String text = StrUtil.trimToEmpty(element.getText());
            if (isMeaningfulPictureText(text, image.getCaption())) {
                return text;
            }
        }
        return "";
    }

    /** 判断元素文本是否为有效图片文字：排除占位符与"仅等于图注"的情况（那不是 OCR 内容） */
    private boolean isMeaningfulPictureText(String text, String caption) {
        if (StrUtil.isBlank(text) || IMAGE_PLACEHOLDER.equals(text.trim())) {
            return false;
        }
        String normalizedCaption = StrUtil.trimToEmpty(caption);
        if (StrUtil.isBlank(normalizedCaption)) {
            return true;
        }
        return !normalizedCaption.equals(text.trim());
    }

    private boolean shouldUseVisionFallback(Long visionModelConfigId, boolean skipRemainingVisionOcr) {
        return !skipRemainingVisionOcr
                && isVisionOcrFallbackEnabled()
                && visionModelConfigId != null;
    }

    private boolean isVisionOcrFallbackEnabled() {
        return doclingProperties != null && doclingProperties.isVisionOcrFallbackEnabled();
    }

    /** 解析视觉回退模型：只认模型管理中标记的默认视觉模型，知识库配置的普通对话模型不用于 OCR */
    private Long resolveVisionModelConfigId(RagConfigDO config) {
        if (config != null && config.getModelParams() != null && config.getModelParams().getModelId() != null) {
            log.warn("Docling vision OCR fallback ignores normal chat model config. "
                    + "Please mark a vision-capable fallback model in model management.");
        }
        if (visionModelConfigResolver == null) {
            return null;
        }
        ModelConfigInfoDTO defaultVisionModel = visionModelConfigResolver.getDefaultVisionChatModel();
        return defaultVisionModel == null ? null : defaultVisionModel.getId();
    }

    private boolean hasOcrText(List<DoclingParseResult.DoclingImage> images) {
        for (DoclingParseResult.DoclingImage image : images) {
            if (image != null && StrUtil.isNotBlank(image.getOcrText())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIndexableOcrText(List<DoclingParseResult.DoclingImage> images) {
        for (DoclingParseResult.DoclingImage image : images) {
            if (image != null && isIndexableOcrText(image.getOcrText())) {
                return true;
            }
        }
        return false;
    }

    private long countImagesWithOcrText(List<DoclingParseResult.DoclingImage> images) {
        if (images == null || images.isEmpty()) {
            return 0;
        }
        return images.stream()
                .filter(image -> image != null && StrUtil.isNotBlank(image.getOcrText()))
                .count();
    }

    /** 把各图片的 OCR 文本合并到对应 picture 元素；找不到宿主元素的图片追加为独立元素，保证文字进入切块 */
    private void mergeOcrTextIntoElements(DoclingParseResult result) {
        Set<Integer> mergedImageIndexes = new HashSet<>();
        List<DoclingParseResult.DoclingElement> elements = result.getElements();
        List<DoclingParseResult.DoclingImage> images = result.getImages();
        if (elements != null) {
            for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
                DoclingParseResult.DoclingImage image = images.get(imageIndex);
                if (image == null || !isIndexableOcrText(image.getOcrText())) {
                    continue;
                }
                if (mergeOcrTextIntoExistingElement(elements, image)) {
                    mergedImageIndexes.add(imageIndex);
                }
            }
        }
        appendStandaloneOcrElements(result, mergedImageIndexes);
    }

    private boolean mergeOcrTextIntoExistingElement(List<DoclingParseResult.DoclingElement> elements,
                                                    DoclingParseResult.DoclingImage image) {
        for (DoclingParseResult.DoclingElement element : elements) {
            if (!isPictureElementForImage(element, image)) {
                continue;
            }
            element.setText(mergeText(element.getText(), image.getCaption(), image.getOcrText()));
            return true;
        }
        return false;
    }

    /** 图片与元素的匹配规则：优先按 elementIndex 精确匹配，否则按页码 + 图注/占位符启发式匹配 */
    private boolean isPictureElementForImage(DoclingParseResult.DoclingElement element,
                                             DoclingParseResult.DoclingImage image) {
        if (element == null || !TYPE_PICTURE.equals(element.getType())) {
            return false;
        }
        if (image.getElementIndex() != null) {
            return image.getElementIndex().equals(element.getIndex());
        }
        if (image.getPage() != null && element.getPage() != null && !image.getPage().equals(element.getPage())) {
            return false;
        }
        String text = StrUtil.blankToDefault(element.getText(), "");
        if (StrUtil.isBlank(image.getCaption())) {
            return IMAGE_PLACEHOLDER.equals(text.trim());
        }
        return text.contains(image.getCaption())
                || IMAGE_PLACEHOLDER.equals(text.trim());
    }

    /** 合并元素文本与 OCR 文本：占位符先替换为图注，已包含 OCR 内容则不重复拼接 */
    private String mergeText(String elementText, String caption, String ocrText) {
        String baseText = StrUtil.blankToDefault(elementText, "");
        if (IMAGE_PLACEHOLDER.equals(baseText.trim())) {
            baseText = StrUtil.blankToDefault(caption, "");
        }
        if (StrUtil.isBlank(baseText)) {
            return ocrText;
        }
        if (baseText.contains(ocrText)) {
            return baseText;
        }
        return baseText + TEXT_SEPARATOR + ocrText;
    }

    private void appendStandaloneOcrElements(DoclingParseResult result, Set<Integer> mergedImageIndexes) {
        List<DoclingParseResult.DoclingImage> images = result.getImages();
        if (images == null || images.isEmpty()) {
            return;
        }
        List<DoclingParseResult.DoclingElement> elements = ensureElements(result);
        int nextIndex = resolveNextElementIndex(elements);
        for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
            if (mergedImageIndexes.contains(imageIndex)) {
                continue;
            }
            DoclingParseResult.DoclingImage image = images.get(imageIndex);
            if (image == null || !isIndexableOcrText(image.getOcrText())) {
                continue;
            }
            elements.add(DoclingParseResult.DoclingElement.builder()
                    .index(nextIndex++)
                    .type(TYPE_PICTURE)
                    .text(mergeText("", image.getCaption(), image.getOcrText()))
                    .page(image.getPage())
                    .level(0)
                    .build());
        }
    }

    private List<DoclingParseResult.DoclingElement> ensureElements(DoclingParseResult result) {
        if (result.getElements() != null) {
            return result.getElements();
        }
        List<DoclingParseResult.DoclingElement> elements = new ArrayList<>();
        result.setElements(elements);
        return elements;
    }

    private int resolveNextElementIndex(List<DoclingParseResult.DoclingElement> elements) {
        int nextIndex = 0;
        if (elements == null) {
            return nextIndex;
        }
        for (DoclingParseResult.DoclingElement element : elements) {
            if (element != null && element.getIndex() != null) {
                nextIndex = Math.max(nextIndex, element.getIndex() + 1);
            }
        }
        return nextIndex;
    }

    private boolean isIndexableOcrText(String ocrText) {
        return imageOcrQualityEvaluator == null || imageOcrQualityEvaluator.isIndexable(ocrText);
    }
}
