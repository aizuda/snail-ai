package com.aizuda.snail.ai.features.rag.strategy.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParser getParser(String fileType) {
        return parsers.stream()
                .filter(p -> p.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + fileType));
    }
}
