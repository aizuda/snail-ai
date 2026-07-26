package com.aizuda.snail.ai.admin.vo.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文本文件向量化请求：文件名与文件路径。
 *
 * @author opensnail
 * @since 2025-07-12
 */
@Data
public class TxtEmbeddingRequestVO {
    private String fileName;
    @NotBlank
    private String filePath;
}
