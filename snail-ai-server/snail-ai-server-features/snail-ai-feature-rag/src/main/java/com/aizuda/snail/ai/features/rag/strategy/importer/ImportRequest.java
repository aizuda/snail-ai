package com.aizuda.snail.ai.features.rag.strategy.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档导入请求：承载知识库 ID 与来源（上传文件 / URL / 文本）及其元信息。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportRequest {

    /**
     * 目标知识库 ID
     */
    private Long ragId;

    /**
     * 上传文件（UPLOAD 来源）
     */
    private MultipartFile file;

    /**
     * 远程文档 URL（URL 来源）
     */
    private String url;

    /**
     * 文本内容（TEXT 来源）
     */
    private String textContent;

    /**
     * 文档名称（可选，缺省时从来源推导）
     */
    private String name;

    /**
     * 来源类型（UPLOAD / URL / TEXT）
     */
    private String sourceType;
}
