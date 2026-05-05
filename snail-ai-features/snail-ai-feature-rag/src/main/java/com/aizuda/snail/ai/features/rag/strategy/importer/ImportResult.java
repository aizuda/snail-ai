package com.aizuda.snail.ai.features.rag.strategy.importer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportResult {

    private String fileName;

    private String fileType;

    private String sourceType;

    private long fileSize;

    /**
     * SHA-256 hex digest of the imported content (for deduplication).
     */
    private String contentHash;

    /**
     * Raw file bytes — used for resource module upload.
     * Only populated for file-based imports (UPLOAD, URL).
     * null for TEXT source type.
     */
    @JsonIgnore
    private transient byte[] rawBytes;
}
