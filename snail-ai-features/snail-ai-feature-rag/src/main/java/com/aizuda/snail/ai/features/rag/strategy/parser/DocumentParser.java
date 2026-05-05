package com.aizuda.snail.ai.features.rag.strategy.parser;

import java.io.InputStream;

public interface DocumentParser {

    boolean supports(String fileType);

    String parse(InputStream inputStream);
}
