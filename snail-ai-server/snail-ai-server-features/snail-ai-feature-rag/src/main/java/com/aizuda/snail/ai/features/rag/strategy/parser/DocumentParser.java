package com.aizuda.snail.ai.features.rag.strategy.parser;

import java.io.InputStream;

/**
 * 文档解析器：将不同类型的文档输入流解析为纯文本，供后续切片与向量化使用。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface DocumentParser {

    /**
     * 是否支持解析指定文件类型。
     *
     * @param fileType 文件类型（如 txt、md、pdf）
     * @return 支持返回 true
     */
    boolean supports(String fileType);

    /**
     * 将文档输入流解析为纯文本。
     *
     * @param inputStream 文档输入流
     * @return 解析后的纯文本
     */
    String parse(InputStream inputStream);
}
