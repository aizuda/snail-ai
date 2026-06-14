package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;

import java.io.InputStream;

/**
 * 文档内容解析器 SPI
 * 负责根据文档的存储方式加载文件内容流。
 * 默认实现走 RAG 本地存储（历史数据降级），可被 admin 层覆盖以使用资源库。
 */
public interface DocumentContentResolver {

    /**
     * 加载文档对应的文件 InputStream。
     * 对于 TEXT 类型文档（内容存储在 content 字段），返回 null。
     */
    InputStream resolve(RagDocumentPO document);
}
