package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.aizuda.snail.ai.features.rag.docling.DoclingResponseParser;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParseResult;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoclingImageOcrApplierTest {

    @Test
    void applyUsesPaddleOcrAndRebuildsChunkContentBeforeIndexing() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> "diagram text",
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(1)
                .type("picture")
                .text("Architecture")
                .page(1)
                .level(0)
                .build());
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .caption("Architecture")
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("[Image: Architecture]")
                .markdownContent("[Image: Architecture]")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals("diagram text", doclingResult.getImages().get(0).getOcrText());
        assertTrue(parseResult.getContent().contains("diagram text"));
        assertEquals(parseResult.getContent(), parseResult.getMarkdownContent());
    }

    @Test
    void applyAddsStandaloneOcrTextWhenDoclingHasImageButNoPictureElement() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> "scanned page text",
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .page(2)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals("scanned page text", doclingResult.getImages().get(0).getOcrText());
        assertTrue(parseResult.getContent().contains("scanned page text"));
    }

    @Test
    void applyAddsStandaloneOcrTextWhenDoclingElementsAreNull() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> "null elements image text",
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .elements(null)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .elementIndex(1)
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals("null elements image text", doclingResult.getImages().get(0).getOcrText());
        assertTrue(parseResult.getContent().contains("null elements image text"));
    }

    @Test
    void applyKeepsLowQualityOcrOnImageButDoesNotMergeIntoChunkContent() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        String noisyOcrText = "@@@\n1\n2\n-\n///";
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> noisyOcrText,
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .page(2)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(noisyOcrText, doclingResult.getImages().get(0).getOcrText());
        assertFalse(parseResult.getContent().contains(noisyOcrText));
    }

    @Test
    void applyMergesReadableOcrIntoChunkContent() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        String readableOcrText = "系统整体架构包含服务层、向量库和大模型调用链路。";
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> readableOcrText,
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .page(2)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(readableOcrText, doclingResult.getImages().get(0).getOcrText());
        assertTrue(parseResult.getContent().contains(readableOcrText));
    }

    @Test
    void applyDoesNotReuseFirstPictureTextForMultipleUncaptionedImagesOnSamePage() {
        AtomicInteger paddleCallCount = new AtomicInteger();
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> "image-" + paddleCallCount.incrementAndGet() + "-text",
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(1)
                .type("picture")
                .text("[image]")
                .page(1)
                .level(0)
                .build());
        doclingResult.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(2)
                .type("picture")
                .text("[image]")
                .page(1)
                .level(0)
                .build());
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2Ux")
                .page(1)
                .build());
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2Uy")
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(2, paddleCallCount.get());
        assertEquals("image-1-text", doclingResult.getImages().get(0).getOcrText());
        assertEquals("image-2-text", doclingResult.getImages().get(1).getOcrText());
        assertEquals("image-1-text", doclingResult.getElements().get(0).getText());
        assertEquals("image-2-text", doclingResult.getElements().get(1).getText());
    }

    @Test
    void applyUsesPaddleBatchOcrForImagesWithoutExistingText() {
        AtomicInteger batchCallCount = new AtomicInteger();
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        ImageOcrHandler paddleHandler = new ImageOcrHandler() {
            @Override
            public String recognize(ImageOcrRequest request) {
                throw new AssertionError("single image OCR should not be used");
            }

            @Override
            public List<PaddleImageOcrResult> recognizeBatch(List<ImageOcrRequest> requests) {
                batchCallCount.incrementAndGet();
                return requests.stream()
                        .map(request -> PaddleImageOcrResult.success(request, "batch-image-" + request.getImageIndex()))
                        .toList();
            }
        };
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                paddleHandler,
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2Ux")
                .page(1)
                .build());
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2Uy")
                .page(2)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(1, batchCallCount.get());
        assertEquals("batch-image-1", doclingResult.getImages().get(0).getOcrText());
        assertEquals("batch-image-2", doclingResult.getImages().get(1).getOcrText());
        assertTrue(parseResult.getContent().contains("batch-image-1"));
        assertTrue(parseResult.getContent().contains("batch-image-2"));
    }

    @Test
    void applySkipsPaddleOcrWhenDoclingPictureElementAlreadyHasText() {
        AtomicInteger paddleCallCount = new AtomicInteger();
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> {
                    paddleCallCount.incrementAndGet();
                    return "should not call";
                },
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(1)
                .type("picture")
                .text("existing picture text")
                .page(1)
                .level(0)
                .build());
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .elementIndex(1)
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("[Image: existing picture text]")
                .markdownContent("[Image: existing picture text]")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(0, paddleCallCount.get());
        assertEquals("existing picture text", doclingResult.getImages().get(0).getOcrText());
        assertTrue(parseResult.getContent().contains("existing picture text"));
        assertFalse(parseResult.getContent().contains("should not call"));
    }

    @Test
    void applySkipsImageOcrWhenKnowledgeSwitchDisabled() {
        AtomicInteger paddleCallCount = new AtomicInteger();
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> {
                    paddleCallCount.incrementAndGet();
                    return "should not call";
                },
                request -> "",
                () -> null,
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(RagConfigDO.builder().build(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(0, paddleCallCount.get());
        assertEquals("body", parseResult.getContent());
    }

    @Test
    void applyUsesDefaultVisionModelFromModelManagement() {
        AtomicLong usedModelConfigId = new AtomicLong();
        DoclingProperties properties = new DoclingProperties();
        properties.setVisionOcrFallbackEnabled(true);
        DoclingImageOcrApplier applier = new DoclingImageOcrApplier(
                new DoclingResponseParser(),
                properties,
                request -> "",
                request -> {
                    usedModelConfigId.set(request.getModelConfigId());
                    return "vision fallback text";
                },
                () -> ModelConfigInfoDTO.builder().id(88L).build(),
                new ImageOcrQualityEvaluator());
        DoclingParseResult doclingResult = DoclingParseResult.builder()
                .success(true)
                .build();
        doclingResult.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64("aW1hZ2U=")
                .page(1)
                .build());
        DocumentParseResult parseResult = DocumentParseResult.builder()
                .content("body")
                .markdownContent("body")
                .doclingResult(doclingResult)
                .build();

        applier.apply(imageOcrConfig(), RagDocumentPO.builder().id(10L).build(), parseResult);

        assertEquals(88L, usedModelConfigId.get());
        assertEquals("vision fallback text", doclingResult.getImages().get(0).getOcrText());
    }

    private RagConfigDO imageOcrConfig() {
        return RagConfigDO.builder()
                .chunkParams(RagConfigDO.ChunkParams.builder()
                        .imageOcr(true)
                        .build())
                .build();
    }
}
