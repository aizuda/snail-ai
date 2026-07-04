package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.features.rag.enums.DocumentParseEngineEnum;

import java.io.InputStream;

/**
 * 文档解析器：将不同类型的文档输入流解析为纯文本，供后续切片与向量化使用。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface DocumentParser {

    /**
     * 解析器类型。默认解析器与原有文件类型解析器保持兼容。
     */
    default String parserType() {
        return DocumentParseEngineEnum.DEFAULT.getValue();
    }

    /**
     * 是否支持解析指定文件类型。
     *
     * @param fileType 文件类型（如 txt、md、pdf）
     * @return 支持返回 true
     */
    boolean supports(String fileType);

    /**
     * 解析文档请求上下文。默认委托到旧的输入流解析方法，保持现有解析器兼容。
     *
     * @param request 解析请求
     * @return 解析结果
     */
    default DocumentParseResult parse(DocumentParseRequest request) {
        return DocumentParseResult.ofContent(parse(request.getInputStream()));
    }

    /**
     * 将文档输入流解析为纯文本。
     *
     * @param inputStream 文档输入流
     * @return 解析后的纯文本
     */
    String parse(InputStream inputStream);
}
