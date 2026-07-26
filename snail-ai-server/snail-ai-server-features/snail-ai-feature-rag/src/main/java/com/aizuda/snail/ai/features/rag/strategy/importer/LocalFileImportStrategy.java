package com.aizuda.snail.ai.features.rag.strategy.importer;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 本地上传文件导入策略：读取 MultipartFile 字节、计算内容哈希并组装导入结果。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
public class LocalFileImportStrategy implements DocumentImportStrategy {

    @Override
    public boolean supports(String sourceType) {
        return DocumentSourceTypeEnum.UPLOAD.getValue().equalsIgnoreCase(sourceType) || sourceType == null;
    }

    @Override
    public ImportResult importDocument(ImportRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required for UPLOAD import");
        }

        String originalName = file.getOriginalFilename();
        String fileType = extractFileType(originalName);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new SnailAiException("Failed to read uploaded file", e);
        }

        String contentHash = ContentHashUtil.sha256Hex(fileBytes);

        log.info("RAG 本地文件导入完成 name={} type={} size={}B", originalName, fileType, fileBytes.length);
        return ImportResult.builder()
                .fileName(originalName)
                .fileType(fileType)
                .sourceType(DocumentSourceTypeEnum.UPLOAD.getValue())
                .contentHash(contentHash)
                .rawBytes(fileBytes)
                .fileSize(file.getSize())
                .build();
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
