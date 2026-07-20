package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.features.rag.pipeline.DocumentPipeline;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import com.aizuda.snail.ai.vector.storage.vector.api.IndexNameBuilder;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 文档处理编排器，负责重新解析触发（状态流转回 PENDING）、文档级清理
 * （向量 / 搜索引擎索引 / 资源文件 / 解析图片 / 切片 / 文档行）以及单切片向量化的触发，
 * 解析与双写的具体实现仍由 feature-rag 模块的 {@link DocumentPipeline} 承担。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentProcessOrchestrator {

    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagMapper knowledgeMapper;
    private final VectorStoreFactory vectorStoreFactory;
    private final DocumentPipeline documentPipeline;
    private final ResourceService resourceService;

    /**
     * 重新处理文档：状态重置为待处理并清空错误信息，触发重新解析/向量化
     *
     * @param documentId 文档 ID
     */
    public void reprocess(Long documentId) {
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc == null) {
            throw new SnailAiException("Document not found: " + documentId);
        }
        doc.setStatus(RagDocumentStatus.PENDING.getStatus());
        doc.setErrorMsg(null);
        ragDocumentMapper.updateById(doc);
        log.info("文档重新处理 documentId={} ragId={}", documentId, doc.getRagId());
    }

    /**
     * 清理文档及其相关资源（向量 / 搜索引擎索引 / 资源库 / 解析图片 / chunks / 文档行）。
     * 同时供普通删除与 OVERWRITE 上传链路复用；事务边界由调用方决定（删除接口带事务，覆盖上传不带）。
     *
     * @param documentId 文档 ID
     */
    public void cleanupDocument(Long documentId) {
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc == null) {
            return;
        }

        RagPO knowledge = knowledgeMapper.selectById(doc.getRagId());
        if (knowledge != null) {
            // 搜索引擎索引清理（在删 chunks 之前，因为需要 chunk ID 列表）
            try {
                documentPipeline.deleteDocumentFromSearchEngine(documentId);
            } catch (Exception e) {
                log.warn("Failed to clean search engine for document: {}", documentId, e);
            }
            try {
                SnailAiVectorStore snailAiVectorStore = vectorStoreFactory.create(knowledge);
                String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
                List<RagChunkPO> chunks = ragChunkMapper.selectList(
                        new LambdaQueryWrapper<RagChunkPO>()
                                .eq(RagChunkPO::getDocumentId, documentId)
                                .isNotNull(RagChunkPO::getVectorId));
                // 只删除没有被其他文档的 chunk 引用的向量
                List<String> vectorIdsToDelete = new ArrayList<>();
                for (RagChunkPO chunk : chunks) {
                    if (StrUtil.isBlank(chunk.getVectorId())) {
                        continue;
                    }
                    Long refCount = ragChunkMapper.selectCount(
                            new LambdaQueryWrapper<RagChunkPO>()
                                    .eq(RagChunkPO::getRagId, doc.getRagId())
                                    .eq(RagChunkPO::getVectorId, chunk.getVectorId())
                                    .ne(RagChunkPO::getDocumentId, documentId));
                    if (refCount == 0) {
                        vectorIdsToDelete.add(chunk.getVectorId());
                    }
                }
                if (!vectorIdsToDelete.isEmpty()) {
                    snailAiVectorStore.delete(indexName, vectorIdsToDelete);
                }
            } catch (Exception e) {
                log.warn("Failed to clean vector store for document: {}", documentId, e);
            }
        }

        // 清理资源库文件
        if (doc.getResourceId() != null) {
            try {
                resourceService.delete(doc.getResourceId());
            } catch (Exception e) {
                log.warn("Failed to delete resource for document: {}", documentId, e);
            }
        }

        cleanDocumentImages(documentId);
        ragChunkMapper.delete(new LambdaQueryWrapper<RagChunkPO>().eq(RagChunkPO::getDocumentId, documentId));
        ragDocumentMapper.deleteById(documentId);
        log.info("清理文档完成 documentId={} ragId={}", documentId, doc.getRagId());
    }

    /**
     * 新增切片后触发向量化（失败仅记录告警，不阻断切片落库）
     *
     * @param chunk     新增的切片
     * @param knowledge 所属知识库
     */
    public void embedChunk(RagChunkPO chunk, RagPO knowledge) {
        try {
            documentPipeline.embedSingleChunk(chunk, knowledge);
        } catch (Exception e) {
            log.warn("新增切片向量化失败, chunkId={}", chunk.getId(), e);
        }
    }

    /**
     * 编辑切片后触发重新向量化（知识库不存在时静默跳过，失败仅记录告警）
     *
     * @param chunk 更新后的切片
     */
    public void reEmbedChunk(RagChunkPO chunk) {
        RagPO knowledge = knowledgeMapper.selectById(chunk.getRagId());
        if (knowledge == null) {
            return;
        }
        try {
            documentPipeline.reEmbedSingleChunk(chunk, knowledge);
        } catch (Exception e) {
            log.warn("编辑切片向量化失败, chunkId={}", chunk.getId(), e);
        }
    }

    /**
     * 删除切片前清理其向量（无向量 ID 时跳过，失败仅记录告警）
     *
     * @param chunk 待删除的切片
     */
    public void deleteChunkVector(RagChunkPO chunk) {
        if (StrUtil.isBlank(chunk.getVectorId())) {
            return;
        }
        try {
            documentPipeline.deleteChunkVector(chunk);
        } catch (Exception e) {
            log.warn("Failed to delete vector for chunk: {}", chunk.getId(), e);
        }
    }

    /**
     * 文档删除或覆盖上传时同步清理 Docling 图片资源和图片关系表。
     */
    private void cleanDocumentImages(Long documentId) {
        List<RagDocumentImagePO> images = ragDocumentImageMapper.selectList(
                new LambdaQueryWrapper<RagDocumentImagePO>().eq(RagDocumentImagePO::getDocumentId, documentId));
        for (RagDocumentImagePO image : images) {
            if (image.getResourceId() == null) {
                continue;
            }
            try {
                resourceService.delete(image.getResourceId());
            } catch (Exception e) {
                log.warn("Failed to delete document image resource. documentId={} resourceId={}",
                        documentId, image.getResourceId(), e);
            }
        }
        ragDocumentImageMapper.delete(
                new LambdaQueryWrapper<RagDocumentImagePO>().eq(RagDocumentImagePO::getDocumentId, documentId));
    }
}
