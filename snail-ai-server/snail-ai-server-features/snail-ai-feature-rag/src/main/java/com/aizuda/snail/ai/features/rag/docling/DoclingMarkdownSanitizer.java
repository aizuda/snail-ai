package com.aizuda.snail.ai.features.rag.docling;

import java.util.regex.Pattern;

/**
 * Docling Markdown 内容清理。
 *
 * @author opensnail
 * @since 2026-06-30
 */
public final class DoclingMarkdownSanitizer {

    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(data:image/[^)]+\\)");
    private static final Pattern EXTRA_BLANK_LINE_PATTERN = Pattern.compile("\\n{3,}");

    private DoclingMarkdownSanitizer() {
    }

    public static String cleanBase64Images(String markdown) {
        if (markdown == null) {
            return null;
        }
        String withoutImages = BASE64_IMAGE_PATTERN.matcher(markdown).replaceAll("[image]");
        return EXTRA_BLANK_LINE_PATTERN.matcher(withoutImages).replaceAll("\n\n").trim();
    }
}
