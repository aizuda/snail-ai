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
 * RAG文本块持久化对象
 * 表: snail_ai_rag_chunk
 *
 * 表示知识库文档分割后的最小语义单元
 * 每个chunk是一个可独立向量化和检索的文本片段
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_rag_chunk")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagChunkPO {

    /**
     * Chunk ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * RAG ID (外键)
     * 关联到 snail_ai_rag.id
     * 该 chunk 所属的 RAG
     */
    @TableField("rag_id")
    private Long ragId;

    /**
     * 文档ID (外键)
     * 关联到 snail_ai_rag_document.id
     * 该chunk来源的文档
     */
    private Long documentId;

    /**
     * 段落索引
     * 该chunk在其所属文档中的段落序号（0-based）
     * 用于排序和恢复原始结构
     */
    private Integer paragraphIndex;

    /**
     * Chunk索引
     * 该chunk在其所属段落内的序号（0-based）
     * 多个chunk可能来自同一段落（如果段落很长）
     */
    private Integer chunkIndex;

    /**
     * Chunk文本内容
     * 该文本块的完整内容
     * 长度由分割规则和配置决定
     */
    private String content;

    /**
     * Token数量
     * 该chunk中的token消耗数量
     * 用于成本计算和大小限制
     */
    private Integer tokenCount;

    /**
     * 向量ID (外键)
     * 向量库中该chunk对应向量的唯一标识
     * 用于向量检索和更新
     */
    private String vectorId;

    /**
     * Chunk内容SHA-256哈希
     * 用于 Chunk 级去重: 同一 RAG 内相同内容的 chunk 共享向量
     */
    private String contentHash;

    /**
     * 创建时间
     * chunk首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * chunk最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
