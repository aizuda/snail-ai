package com.aizuda.snail.ai.features.rag.strategy.importer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentImportFactory {

    private final List<DocumentImportStrategy> strategies;

    public DocumentImportStrategy getStrategy(String sourceType) {
        return strategies.stream()
                .filter(s -> s.supports(sourceType))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unsupported source type: " + sourceType));
    }
}
