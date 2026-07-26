package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEvidenceMatcherTest {

    private static final String CERTIFICATE_NO = "合格证编号";

    private final ImageEvidenceMatcher matcher = new ImageEvidenceMatcher();

    @Test
    void matchReturnsScoreForOcrPartialQuery() {
        RagDocumentImagePO image = RagDocumentImagePO.builder()
                .ocrText(CERTIFICATE_NO + " 有效期 发证日期")
                .build();

        ImageEvidenceMatcher.ImageEvidence evidence = matcher.match(image, CERTIFICATE_NO + "在哪里");

        assertTrue(evidence.score() > 0);
        assertEquals("ocrText", evidence.fields().get(0));
        assertTrue(evidence.terms().contains(CERTIFICATE_NO));
    }

    @Test
    void buildDirectMatchReasonUsesLongestMatchedTerm() {
        ImageEvidenceMatcher.ImageEvidence evidence = new ImageEvidenceMatcher.ImageEvidence(
                6,
                java.util.List.of("合格", CERTIFICATE_NO),
                java.util.List.of("ocrText"));

        assertEquals("OCR 命中：" + CERTIFICATE_NO, matcher.buildDirectMatchReason(evidence));
    }
}
