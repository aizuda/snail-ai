package com.aizuda.snail.ai.admin.vo.rag;

import lombok.Data;

/**
 * author: opensnail date: 2025-07-18
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
