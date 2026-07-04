package com.aizuda.snail.ai.features.rag.docling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DoclingMarkdownSanitizerTest {

    @Test
    void cleanBase64ImagesReplacesEmbeddedImagesAndCollapsesBlankLines() {
        String markdown = """
                # Title

                ![chart](data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==)


                Body
                """;

        String cleaned = DoclingMarkdownSanitizer.cleanBase64Images(markdown);

        assertEquals("# Title\n\n[image]\n\nBody", cleaned);
    }

    @Test
    void cleanBase64ImagesReturnsNullForNullInput() {
        assertNull(DoclingMarkdownSanitizer.cleanBase64Images(null));
    }
}
