package com.aizuda.snail.ai.admin.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 文档段落响应：切块内容及创建 / 更新时间。
 *
 * @author opensnail
 * @since 2025-07-19
 */
@Data
public class ParagraphResponseVO {

    private Long id;

    private Long documentId;

    private String content;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
