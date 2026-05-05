package com.aizuda.snail.ai.features.rag.strategy.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentImportFactory {

    private final List<DocumentImportStrategy> strategies;

    public DocumentImportStrategy getStrategy(String sourceType) {
        return strategies.stream()
                .filter(s -> s.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported source type: " + sourceType));
    }
}
