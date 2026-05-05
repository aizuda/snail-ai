package com.aizuda.snail.ai.features.rag.strategy.importer;

public interface DocumentImportStrategy {

    boolean supports(String sourceType);

    ImportResult importDocument(ImportRequest request);
}
