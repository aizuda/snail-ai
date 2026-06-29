package com.aizuda.snail.ai.features.rag.strategy.importer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档导入结果：文件名、类型、大小、内容哈希（去重用）与原始字节（仅文件类来源）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportResult {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型（扩展名，如 pdf、docx）
     */
    private String fileType;

    /**
     * 来源类型（UPLOAD / URL / TEXT）
     */
    private String sourceType;

    /**
     * 文件大小（字节）
     */
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
