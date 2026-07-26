package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 切片视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagChunkResponseVO {

    /** 切片 ID */
    private Long id;

    /** 知识库 ID */
    private Long ragId;

    /** 所属文档 ID */
    private Long documentId;

    /** 段落序号 */
    private Integer paragraphIndex;

    /** 切片序号 */
    private Integer chunkIndex;

    /** 切片内容 */
    private String content;

    /** Token 数量 */
    private Integer tokenCount;

    /** 向量 ID */
    private String vectorId;

    /** 所属文档名称 */
    private String documentName;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
