package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoclingImageSavePolicyTest {

    @Test
    void usesGlobalDoclingImageLimitsWhenKnowledgeDoesNotOverride() {
        DoclingProperties properties = new DoclingProperties();
        properties.setMaxImageCount(2);
        properties.setMaxImageBytes(1024L);

        DoclingImageSavePolicy policy = DoclingImageSavePolicy.from(properties, new RagConfigDO());

        assertFalse(policy.isCountExceeded(1));
        assertTrue(policy.isCountExceeded(2));
        assertFalse(policy.isOversized(1024));
        assertTrue(policy.isOversized(1025));
    }

    @Test
    void knowledgeDoclingImageLimitsOverrideGlobalDefaults() {
        DoclingProperties properties = new DoclingProperties();
        properties.setMaxImageCount(10);
        properties.setMaxImageBytes(4096L);
        RagConfigDO config = RagConfigDO.builder()
                .parseParams(RagConfigDO.ParseParams.builder()
                        .docling(RagConfigDO.DoclingParams.builder()
                                .maxImageCount(3)
                                .maxImageBytes(2048L)
                                .build())
                        .build())
                .build();

        DoclingImageSavePolicy policy = DoclingImageSavePolicy.from(properties, config);

        assertFalse(policy.isCountExceeded(2));
        assertTrue(policy.isCountExceeded(3));
        assertFalse(policy.isOversized(2048));
        assertTrue(policy.isOversized(2049));
    }

    @Test
    void invalidKnowledgeDoclingImageLimitsFallBackToGlobalDefaults() {
        DoclingProperties properties = new DoclingProperties();
        properties.setMaxImageCount(4);
        properties.setMaxImageBytes(4096L);
        RagConfigDO config = RagConfigDO.builder()
                .parseParams(RagConfigDO.ParseParams.builder()
                        .docling(RagConfigDO.DoclingParams.builder()
                                .maxImageCount(0)
                                .maxImageBytes(-1L)
                                .build())
                        .build())
                .build();

        DoclingImageSavePolicy policy = DoclingImageSavePolicy.from(properties, config);

        assertTrue(policy.isCountExceeded(4));
        assertTrue(policy.isOversized(4097));
    }
}
