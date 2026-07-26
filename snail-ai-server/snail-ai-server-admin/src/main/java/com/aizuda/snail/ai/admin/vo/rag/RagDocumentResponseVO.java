package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 文档视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagDocumentResponseVO {

    /** 文档 ID */
    private Long id;

    /** 知识库 ID */
    private Long ragId;

    /** 文档名称 */
    private String name;

    /** 文件类型 */
    private String fileType;

    /** 来源类型 */
    private String sourceType;

    /** 处理状态 */
    private Integer status;

    /** 处理失败时的错误信息 */
    private String errorMsg;

    /** 切片数量 */
    private Integer chunkCount;

    /** Docling 解析页数 */
    private Integer pageCount;

    /** Docling 解析元素数量 */
    private Integer elementCount;

    /** Docling 解析表格数量 */
    private Integer tableCount;

    /** Docling 解析图片数量 */
    private Integer imageCount;

    /** 解析耗时（毫秒） */
    private Integer parseTime;

    /** Docling Markdown 内容 */
    private String mdContent;

    /** Docling 文档元数据（JSON） */
    private String docMetadata;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 关联资源 ID */
    private Long resourceId;

    /** 预览地址 */
    private String previewUrl;

    /** 下载地址 */
    private String downloadUrl;

    /**
     * 决策结果: NEW / SKIP / OVERWRITE
     * REJECT 通过异常返回，不会进入此字段
     */
    private String decision;

    /** 命中的冲突维度: NONE / BY_NAME / BY_CONTENT / BOTH */
    private String matchType;

    /** 冲突的旧文档 ID，仅在 SKIP/OVERWRITE 时返回 */
    private Long conflictDocumentId;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
