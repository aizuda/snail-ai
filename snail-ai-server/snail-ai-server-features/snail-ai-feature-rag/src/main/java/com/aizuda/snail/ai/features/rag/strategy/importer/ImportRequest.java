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

    private Long ragId;

    private MultipartFile file;

    private String url;

    private String textContent;

    private String name;

    private String sourceType;
}
