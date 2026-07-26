package com.aizuda.snail.ai.features.rag.strategy.parser;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.docling.DoclingClient;
import com.aizuda.snail.ai.features.rag.docling.DoclingConvertOptions;
import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.docling.DoclingResponseParser;
import com.aizuda.snail.ai.features.rag.enums.DocumentParseEngineEnum;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

/**
 * Docling 文档解析器。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoclingParser implements DocumentParser {

    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "docx", "xlsx", "xls", "pptx", "html", "md");
    private static final String TEMP_FILE_PREFIX = "snail-ai-docling-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private final DoclingClient doclingClient;
    private final DoclingResponseParser responseParser;

    @Override
    public String parserType() {
        return DocumentParseEngineEnum.DOCLING.getValue();
    }

    @Override
    public boolean supports(String fileType) {
        return doclingClient.isEnabled() && StrUtil.isNotBlank(fileType)
                && SUPPORTED_TYPES.contains(fileType.toLowerCase());
    }

    @Override
    public DocumentParseResult parse(DocumentParseRequest request) {
        if (!doclingClient.isAvailable()) {
            throw new SnailAiException("Docling service is unavailable");
        }
        File tempFile = null;
        try {
            // Docling Serve 接收文件表单，资源流需要先落成带后缀的临时文件。
            tempFile = File.createTempFile(TEMP_FILE_PREFIX, resolveTempSuffix(request));
            FileUtil.writeFromStream(request.getInputStream(), tempFile);
            DoclingConvertOptions options = DoclingConvertOptions.fromConfig(resolveDoclingParams(request.getConfig()));
            DoclingParseResult doclingResult = doclingClient.convert(tempFile, options);
            // 清理 Markdown 中的内嵌 base64 图片，图片本身由 DocumentPipeline 统一保存到资源库。
            String cleanMarkdown = responseParser.toCleanMarkdown(doclingResult);
            return DocumentParseResult.builder()
                    .content(cleanMarkdown)
                    .markdownContent(cleanMarkdown)
                    .docMetadata(doclingResult.getDocMetadata())
                    .pageCount(doclingResult.getPageCount())
                    .elementCount(doclingResult.getElementCount())
                    .tableCount(doclingResult.getTableCount())
                    .imageCount(doclingResult.getImageCount())
                    .parseTime(doclingResult.getParseTime())
                    .doclingResult(doclingResult)
                    .build();
        } catch (Exception e) {
            if (e instanceof SnailAiException snailAiException) {
                throw snailAiException;
            }
            throw new SnailAiException("Failed to parse document with Docling", e);
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("Failed to delete Docling temp file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public String parse(InputStream inputStream) {
        // 兼容旧的 DocumentParser 接口；实际流水线会调用 parse(DocumentParseRequest) 传入配置。
        return parse(DocumentParseRequest.builder()
                .inputStream(inputStream)
                .fileType("pdf")
                .build()).getContent();
    }

    private RagConfigDO.DoclingParams resolveDoclingParams(RagConfigDO config) {
        if (config == null || config.getParseParams() == null) {
            return null;
        }
        return config.getParseParams().getDocling();
    }

    private String resolveTempSuffix(DocumentParseRequest request) {
        String fileType = request.getFileType();
        if (StrUtil.isNotBlank(fileType)) {
            return "." + fileType.toLowerCase();
        }
        String fileName = request.getFileName();
        if (StrUtil.isNotBlank(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return TEMP_FILE_SUFFIX;
    }
}
