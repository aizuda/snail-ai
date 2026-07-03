package com.aizuda.snail.ai.persistence.rag.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 文档解析图片。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@TableName("sai_rag_document_image")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagDocumentImagePO {

    /** 图片记录 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属知识库 ID。 */
    private Long ragId;

    /** 所属文档 ID。 */
    private Long documentId;

    private Long chunkId;

    /** 资源库中的图片资源 ID。 */
    private Long resourceId;

    private Integer imageIndex;

    /** 图片访问地址，来自资源模块回填。 */
    private String imageUrl;

    /** Docling 解析出的图注。 */
    private String caption;

    private String figureNo;

    private String figureTitle;

    private String sectionTitle;

    /** 图片所在原文页码。 */
    private Integer sourcePage;

    private String documentName;

    /** 图片 OCR 文本。 */
    private String ocrText;

    /** 创建时间。 */
    private LocalDateTime createDt;

    /** 更新时间。 */
    private LocalDateTime updateDt;
}
