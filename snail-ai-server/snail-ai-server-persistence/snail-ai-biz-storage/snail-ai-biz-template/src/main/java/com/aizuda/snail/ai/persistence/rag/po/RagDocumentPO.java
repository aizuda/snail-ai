package com.aizuda.snail.ai.persistence.rag.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG文档持久化对象
 * 表: sai_rag_document
 *
 * 表示知识库中的一个完整文档
 * 支持多种文件类型（PDF、Word、TXT等）和存储后端（本地、OSS、S3等）
 * 文档会被分割为多个chunk进行向量化和检索
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_rag_document")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagDocumentPO {

    /**
     * 文档ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * RAG ID (外键)
     * 关联到 sai_rag.id
     * 该文档所属的 RAG
     */
    @TableField("rag_id")
    private Long ragId;

    /**
     * 文档名称
     * 原始文件的名称（不包含路径）
     * 用于前端显示和用户识别
     */
    private String name;

    /**
     * 文件类型
     * 文件的扩展名或MIME类型
     * 例如: pdf, docx, txt, md, html等
     */
    private String fileType;

    /**
     * 源类型
     * 文档的来源方式
     * 可取值: UPLOAD (用户上传), URL (网络获取), API_PROVIDED (API提供)等
     */
    private String sourceType;

    /**
     * 文档状态
     * PENDING: 待处理
     * PROCESSING: 处理中（分割、向量化）
     * COMPLETED: 已完成
     * FAILED: 处理失败
     */
    private Integer status;

    /**
     * 错误信息
     * 处理失败时的错误描述
     * 成功状态下为null
     */
    private String errorMsg;

    /**
     * Chunk数量
     * 该文档分割后生成的chunk总数
     * 用于统计和显示
     */
    private Integer chunkCount;

    /**
     * 文件内容SHA-256哈希
     * 用于文档级去重: 同一 RAG 内相同哈希视为重复
     */
    private String contentHash;

    /**
     * 资源库ID (外键)
     * 关联到 sai_resource.id
     * 文件通过资源模块统一存储
     */
    private Long resourceId;

    /**
     * 创建时间
     * 文档首次上传/创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 文档最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
