package com.aizuda.snail.ai.features.rag.strategy.importer;

/**
 * DocumentImportStrategy
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface DocumentImportStrategy {

    boolean supports(String sourceType);

    ImportResult importDocument(ImportRequest request);
}
