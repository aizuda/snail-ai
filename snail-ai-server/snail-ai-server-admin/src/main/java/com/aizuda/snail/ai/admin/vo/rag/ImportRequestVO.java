package com.aizuda.snail.ai.admin.vo.rag;

import lombok.Data;

/**
 * 文档导入请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class ImportRequestVO {

    private Long docId;
    private String appendPrefix;
    private String appendPost;
    private String resourcePath;
}
