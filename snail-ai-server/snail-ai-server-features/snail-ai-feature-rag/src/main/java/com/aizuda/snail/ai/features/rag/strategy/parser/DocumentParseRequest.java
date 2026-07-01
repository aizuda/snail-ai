package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * 文档解析请求上下文。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Builder
public class DocumentParseRequest {

    /**
     * 原始文档资源流，由调用方负责关闭。
     */
    private InputStream inputStream;

    /**
     * 原始文件名，用于 Docling 临时文件后缀和日志定位。
     */
    private String fileName;

    /**
     * 文件类型，例如 pdf、docx、xlsx。
     */
    private String fileType;

    /**
     * 知识库配置，解析器从 parseParams 中读取解析引擎和 Docling 参数。
     */
    private RagConfigDO config;
}
