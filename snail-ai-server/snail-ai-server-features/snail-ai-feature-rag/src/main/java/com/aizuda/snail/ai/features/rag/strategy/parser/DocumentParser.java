package com.aizuda.snail.ai.features.rag.strategy.parser;

import java.io.InputStream;

/**
 * DocumentParser
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface DocumentParser {

    boolean supports(String fileType);

    String parse(InputStream inputStream);
}
