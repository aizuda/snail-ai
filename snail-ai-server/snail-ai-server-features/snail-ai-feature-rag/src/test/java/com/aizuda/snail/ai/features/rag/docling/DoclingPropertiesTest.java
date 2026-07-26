package com.aizuda.snail.ai.features.rag.docling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoclingPropertiesTest {

    @Test
    void usesSafeDefaultsForDoclingTimeoutsAndImageLimits() {
        DoclingProperties properties = new DoclingProperties();

        assertEquals(3000, properties.getPollIntervalMillis());
        assertEquals(10000, properties.getStatusTimeoutMillis());
        assertEquals(30000, properties.getResultTimeoutMillis());
        assertEquals(100, properties.getMaxImageCount());
        assertEquals(10485760L, properties.getMaxImageBytes());
    }
}
