package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.aizuda.snail.ai.features.rag.enums.DocumentParseEngineEnum;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParseRequest;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParseResult;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParser;
import com.aizuda.snail.ai.features.rag.strategy.parser.DocumentParserFactory;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.search.storage.search.SearchEngineFactory;
import com.aizuda.snail.ai.search.storage.search.api.SearchAddRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDeleteRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDocument;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.constant.SearchMetadataKeys;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import com.aizuda.snail.ai.vector.storage.vector.api.IndexNameBuilder;
import com.aizuda.snail.ai.vector.storage.vector.api.VectorAddRequest;
import com.aizuda.snail.ai.vector.storage.vector.api.VectorDocument;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档处理流水线，负责 RAG 文档的解析、分块、内容去重、双写（向量库 + 搜索引擎）与处理状态流转。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPipeline {

    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagMapper knowledgeMapper;
    private final DocumentParserFactory parserFactory;
    private final DocumentChunkingService documentChunkingService;
    private final VectorStoreFactory vectorStoreFactory;
    private final SearchEngineFactory searchEngineFactory;
    private final ResourceService resourceService;
    private final DoclingProperties doclingProperties;
    private final DoclingImageOcrApplier doclingImageOcrApplier;
    private final DocumentImageChunkMatcher documentImageChunkMatcher;
    private final DocumentImageKnowledgeChunkBuilder documentImageKnowledgeChunkBuilder;

    /**
     * 处理指定文档：抢占 PENDING 状态后解析、分块、双写并更新为最终状态（失败时记录错误）
     *
     * @param documentId 文档 ID
     */
    public void processDocument(Long documentId) {
        RagDocumentPO document = ragDocumentMapper.selectById(documentId);
        if (document == null) {
            log.error("Document not found: {}", documentId);
            return;
        }

        if (!claimPendingDocument(documentId)) {
            log.info("Skip document processing, document is not pending: [{}]", documentId);
            return;
        }

        try {
            RagPO knowledge = knowledgeMapper.selectById(document.getRagId());
            if (knowledge == null) {
                log.error("Knowledge not found: {} for document: {}", document.getRagId(), documentId);
                updateStatus(documentId, RagDocumentStatus.FAILED, "Knowledge not found");
                return;
            }

            log.info("Start processing document: [{}] {}", documentId, document.getName());

            RagConfigDO config = parseKnowledgeConfig(knowledge);
            // 解析阶段会按知识库 parseParams 决定是否使用 Docling，并保留结构化统计信息。
            DocumentParseResult parseResult = parseDocument(document, config);
            doclingImageOcrApplier.apply(config, document, parseResult);
            String content = parseResult.getContent();
            if (StrUtil.isBlank(content)) {
                updateStatus(documentId, RagDocumentStatus.FAILED, "Empty content after parsing");
                return;
            }

            cleanExistingData(knowledge, documentId);

            List<ChunkDTO> chunks = documentChunkingService.chunk(content, knowledge);

            if (chunks.isEmpty()) {
                updateStatus(documentId, RagDocumentStatus.FAILED, "No chunks generated");
                return;
            }

            List<RagChunkPO> chunkPOs = new ArrayList<>();
            for (ChunkDTO chunk : chunks) {
                chunkPOs.add(RagChunkPO.builder()
                        .ragId(knowledge.getId())
                        .documentId(documentId)
                        .paragraphIndex(chunk.getParagraphIndex())
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .tokenCount(chunk.getTokenCount())
                        .contentHash(ContentHashUtil.sha256Hex(chunk.getContent()))
                        .sourceType(RagChunkSourceTypeEnum.TEXT.getValue())
                        .build());
            }
            chunkPOs.addAll(buildImageKnowledgeChunks(config, knowledge, document, parseResult, chunkPOs.size()));

            ragChunkMapper.insert(chunkPOs);
            // ── Dual write: VectorStore + SearchEngine ──
            dualWrite(chunkPOs, knowledge);
            // 图片不进入 chunk 文本，双写成功后再保存为资源，避免失败文档留下半成功图片记录。
            saveDoclingImages(config, document, parseResult, chunkPOs);
            document.setChunkCount(chunkPOs.size());
            applyParseResult(document, parseResult);
            document.setStatus(RagDocumentStatus.SUCCESS.getStatus());
            document.setErrorMsg(null);
            ragDocumentMapper.updateById(document);

            log.info("Document processed successfully: [{}], chunks: {}", documentId, chunkPOs.size());

        } catch (Exception e) {
            log.error("Document processing failed: [{}]", documentId, e);
            updateStatus(documentId, RagDocumentStatus.FAILED, e.getMessage());
        }
    }

    private boolean claimPendingDocument(Long documentId) {
        RagDocumentPO update = new RagDocumentPO();
        update.setStatus(RagDocumentStatus.PROCESSING.getStatus());
        update.setErrorMsg(null);
        return ragDocumentMapper.update(update, new LambdaUpdateWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getId, documentId)
                .eq(RagDocumentPO::getStatus, RagDocumentStatus.PENDING.getStatus())) > 0;
    }

    /**
     * 为单个切片生成向量并写入向量库与搜索引擎（新增切片场景）
     */
    public void embedSingleChunk(RagChunkPO chunk, RagPO knowledge) {
        dualWrite(List.of(chunk), knowledge);
    }

    /**
     * 删除旧向量后重新嵌入（编辑切片场景）
     */
    public void reEmbedSingleChunk(RagChunkPO chunk, RagPO knowledge) {
        if (StrUtil.isNotBlank(chunk.getVectorId())) {
            try {
                SnailAiVectorStore snailAiVectorStore = vectorStoreFactory.create(knowledge);
                String idx = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
                snailAiVectorStore.delete(idx, List.of(chunk.getVectorId()));
            } catch (Exception e) {
                log.warn("Delete old vector failed for chunk: {}", chunk.getId(), e);
            }
            chunk.setVectorId(null);
        }
        deleteSingleFromSearchEngine(chunk);
        dualWrite(List.of(chunk), knowledge);
    }

    /**
     * 删除切片对应的向量（删除切片时调用）
     * 只有当没有其他 chunk 引用同一个 vectorId 时才真正删除向量
     */
    public void deleteChunkVector(RagChunkPO chunk) {
        if (StrUtil.isBlank(chunk.getVectorId())) {
            return;
        }
        RagPO knowledge = knowledgeMapper.selectById(chunk.getRagId());
        if (knowledge == null) {
            return;
        }
        // 引用计数检查：是否有其他 chunk 共享同一 vectorId
        Long refCount = ragChunkMapper.selectCount(
                new LambdaQueryWrapper<RagChunkPO>()
                        .eq(RagChunkPO::getRagId, chunk.getRagId())
                        .eq(RagChunkPO::getVectorId, chunk.getVectorId())
                        .ne(RagChunkPO::getId, chunk.getId()));
        if (refCount > 0) {
            return;
        }
        try {
            SnailAiVectorStore snailAiVectorStore = vectorStoreFactory.create(knowledge);
            String idx = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
            snailAiVectorStore.delete(idx, List.of(chunk.getVectorId()));
            log.debug("RAG 删除切片向量 chunkId={} vectorId={}", chunk.getId(), chunk.getVectorId());
        } catch (Exception e) {
            log.warn("Delete vector failed for chunk: {}", chunk.getId(), e);
        }
        deleteSingleFromSearchEngine(chunk);
    }

    /**
     * 清理文档在搜索引擎中的所有索引记录（供外部调用）。
     *
     * @param documentId 文档 ID
     */
    public void deleteDocumentFromSearchEngine(Long documentId) {
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc == null) {
            return;
        }
        RagPO knowledge = knowledgeMapper.selectById(doc.getRagId());
        if (knowledge == null) {
            return;
        }
        deleteFromSearchEngine(knowledge, documentId);
    }

    // ─────────────────────────────────── private ────────────────────────────────────

    /**
     * 删除文档在搜索引擎中的所有索引记录（按 documentId 过滤后批量删除）。
     */
    private void deleteFromSearchEngine(RagPO knowledge, Long documentId) {
        if (!Boolean.TRUE.equals(knowledge.getSearchEngineEnable())) {
            return;
        }
        try {
            // 查该文档所有 chunk 的 ID，搜索引擎中 ID = String.valueOf(chunkId)
            List<RagChunkPO> chunks = ragChunkMapper.selectList(
                    new LambdaQueryWrapper<RagChunkPO>()
                            .eq(RagChunkPO::getDocumentId, documentId)
                            .eq(RagChunkPO::getRagId, knowledge.getId()));
            if (chunks.isEmpty()) {
                return;
            }
            List<String> ids = chunks.stream()
                    .map(c -> String.valueOf(c.getId()))
                    .toList();
            SearchEngine searchEngine = searchEngineFactory.forStoreInstance(
                    knowledge.getSearchEngineInstanceId());
            String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
            searchEngine.delete(SearchDeleteRequest.builder()
                    .indexName(indexName)
                    .ids(ids)
                    .build());
            log.debug("RAG 搜索引擎删除 {} 个文档 documentId={} ragId={}", ids.size(), documentId, knowledge.getId());
        } catch (Exception e) {
            log.warn("Failed to clean search engine for document: {}", documentId, e);
        }
    }

    /**
     * 删除单个 chunk 在搜索引擎中的索引记录。
     */
    private void deleteSingleFromSearchEngine(RagChunkPO chunk) {
        RagPO knowledge = knowledgeMapper.selectById(chunk.getRagId());
        if (knowledge == null || !Boolean.TRUE.equals(knowledge.getSearchEngineEnable())) {
            return;
        }
        try {
            SearchEngine searchEngine = searchEngineFactory.forStoreInstance(
                    knowledge.getSearchEngineInstanceId());
            String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
            searchEngine.delete(SearchDeleteRequest.builder()
                    .indexName(indexName)
                    .ids(List.of(String.valueOf(chunk.getId())))
                    .build());
            log.debug("RAG 搜索引擎删除单个 chunk chunkId={}", chunk.getId());
        } catch (Exception e) {
            log.warn("Failed to clean search engine for chunk: {}", chunk.getId(), e);
        }
    }

    /**
     * Dual-write: embed chunks → write to VectorStore, and insert into SearchEngine.
     */
    private void dualWrite(List<RagChunkPO> chunkPOs, RagPO knowledge) {
        log.debug("RAG 双写开始 chunks={} ragId={} searchEngine={}",
                chunkPOs.size(), knowledge.getId(), knowledge.getSearchEngineEnable());
        SnailAiVectorStore snailAiVectorStore = vectorStoreFactory.create(knowledge);

        List<List<RagChunkPO>> batches = Lists.partition(chunkPOs, 10);
        for (List<RagChunkPO> batch : batches) {
            processVectorBatch(batch, snailAiVectorStore, knowledge);
        }

        // Write to search engine (if enabled)
        if (!Boolean.TRUE.equals(knowledge.getSearchEngineEnable())) {
            return;
        }
        try {
            SearchEngine searchEngine = searchEngineFactory.forStoreInstance(knowledge.getSearchEngineInstanceId());
            String searchIndexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));

            List<SearchDocument> searchDocs = chunkPOs.stream()
                    .map(c -> SearchDocument.builder()
                            .id(String.valueOf(c.getId()))
                            .content(c.getContent())
                            .metadata(Map.of(
                                    SearchMetadataKeys.RAG_ID, knowledge.getId(),
                                    SearchMetadataKeys.DOCUMENT_ID, c.getDocumentId(),
                                    SearchMetadataKeys.CHUNK_ID, c.getId(),
                                    SearchMetadataKeys.SOURCE_TYPE, resolveChunkSourceType(c)))
                            .build())
                    .toList();
            searchEngine.insert(SearchAddRequest.builder()
                    .indexName(searchIndexName)
                    .documents(searchDocs)
                    .build());
            log.debug("RAG 搜索引擎写入 {} 个文档 ragId={}", searchDocs.size(), knowledge.getId());
        } catch (Exception e) {
            log.warn("SearchEngine insert failed (non-fatal): {}", e.getMessage());
        }
    }

    private void processVectorBatch(List<RagChunkPO> batch, SnailAiVectorStore snailAiVectorStore, RagPO knowledge) {
        List<VectorDocument> toEmbed = new ArrayList<>();
        int reusedCount = 0;

        for (RagChunkPO chunk : batch) {
            // Chunk 级去重：查找同一 RAG 下已有相同内容且已向量化的 chunk
            if (StrUtil.isNotBlank(chunk.getContentHash())) {
                RagChunkPO existing = ragChunkMapper.selectOne(
                        new LambdaQueryWrapper<RagChunkPO>()
                                .eq(RagChunkPO::getRagId, knowledge.getId())
                                .eq(RagChunkPO::getContentHash, chunk.getContentHash())
                                .isNotNull(RagChunkPO::getVectorId)
                                .ne(RagChunkPO::getId, chunk.getId())
                                .last("LIMIT 1"));
                if (existing != null) {
                    // 复用已有向量，不重复 embedding
                    chunk.setVectorId(existing.getVectorId());
                    ragChunkMapper.updateById(chunk);
                    reusedCount++;
                    continue;
                }
            }

            // 需要新建向量
            String vectorId = UUID.randomUUID().toString();
            chunk.setVectorId(vectorId);

            toEmbed.add(VectorDocument.builder()
                    .id(vectorId)
                    .content(chunk.getContent())
                    .metadata(Map.of(
                            "ragId", knowledge.getId(),
                            "documentId", chunk.getDocumentId(),
                            "chunkId", chunk.getId(),
                            "sourceType", resolveChunkSourceType(chunk)))
                    .build());
        }

        if (toEmbed.isEmpty()) {
            log.debug("RAG 向量批写入：本批 {} 个切片全部命中去重复用，跳过 embedding ragId={}",
                    reusedCount, knowledge.getId());
            return;
        }
        String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
        snailAiVectorStore.add(VectorAddRequest.builder()
                .indexName(indexName)
                .documents(toEmbed)
                .build());
        for (RagChunkPO chunk : batch) {
            if (StrUtil.isBlank(chunk.getVectorId())) {
                continue;
            }
            ragChunkMapper.updateById(chunk);
        }
        log.debug("RAG 向量批写入完成 新增 embedding {} 个，复用 {} 个 ragId={}",
                toEmbed.size(), reusedCount, knowledge.getId());
    }

    private DocumentParseResult parseDocument(RagDocumentPO document, RagConfigDO config) {
        InputStream is = resourceService.load(document.getResourceId());
        if (is == null) {
            return DocumentParseResult.ofContent("");
        }
        DocumentParseRequest request = DocumentParseRequest.builder()
                .inputStream(is)
                .fileName(document.getName())
                .fileType(document.getFileType())
                .config(config)
                .build();
        DocumentParser parser = parserFactory.getParser(request);
        try (is) {
            return parser.parse(request);
        } catch (Exception e) {
            if (!isDoclingParser(parser)) {
                throw new SnailAiException("Failed to parse document file: " + document.getId(), e);
            }
            // Docling 是增强能力，失败时保留原有内置解析链路，避免知识库导入被外部服务阻断。
            log.warn("Docling parse failed, fallback to default parser. documentId={}, reason={}",
                    document.getId(), e.getMessage());
            return parseWithFallback(document);
        }
    }

    private DocumentParseResult parseWithFallback(RagDocumentPO document) {
        InputStream fallbackStream = resourceService.load(document.getResourceId());
        if (fallbackStream == null) {
            return DocumentParseResult.ofContent("");
        }
        DocumentParser fallbackParser = parserFactory.getFallbackParser(document.getFileType());
        try (fallbackStream) {
            return fallbackParser.parse(DocumentParseRequest.builder()
                    .inputStream(fallbackStream)
                    .fileName(document.getName())
                    .fileType(document.getFileType())
                    .build());
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse document file: " + document.getId(), e);
        }
    }

    private boolean isDoclingParser(DocumentParser parser) {
        return DocumentParseEngineEnum.DOCLING.getValue().equals(parser.parserType());
    }

    private RagConfigDO parseKnowledgeConfig(RagPO knowledge) {
        if (knowledge == null || StrUtil.isBlank(knowledge.getConfig())) {
            return null;
        }
        try {
            return JsonUtil.parseObject(knowledge.getConfig(), RagConfigDO.class);
        } catch (Exception e) {
            log.warn("Failed to parse knowledge rag config, fallback to defaults. ragId={}, reason={}",
                    knowledge.getId(), e.getMessage());
            return null;
        }
    }

    private void applyParseResult(RagDocumentPO document, DocumentParseResult parseResult) {
        // 默认解析器只写 content；Docling 解析器额外写解析统计和 Markdown/metadata。
        document.setPageCount(parseResult.getPageCount());
        document.setElementCount(parseResult.getElementCount());
        document.setTableCount(parseResult.getTableCount());
        document.setImageCount(parseResult.getImageCount());
        document.setParseTime(parseResult.getParseTime());
        document.setMdContent(parseResult.getMarkdownContent());
        document.setDocMetadata(parseResult.getDocMetadata());
    }

    private void saveDoclingImages(RagConfigDO config, RagDocumentPO document, DocumentParseResult parseResult,
                                   List<RagChunkPO> chunkPOs) {
        // 图片保存受知识库 parseParams.docling.saveImages 控制，默认保存。
        if (!shouldSaveImages(config) || parseResult == null || parseResult.getDoclingResult() == null) {
            return;
        }
        List<DoclingParseResult.DoclingImage> images = parseResult.getDoclingResult().getImages();
        if (images == null || images.isEmpty()) {
            return;
        }
        DoclingImageSavePolicy savePolicy = DoclingImageSavePolicy.from(doclingProperties, config);
        int imageIndex = 0;
        int savedCount = 0;
        for (DoclingParseResult.DoclingImage image : images) {
            imageIndex++;
            if (image == null || StrUtil.isBlank(image.getBase64())) {
                continue;
            }
            if (savePolicy.isCountExceeded(savedCount)) {
                log.warn("Skip remaining Docling images because max count reached. documentId={} maxImageCount={}",
                        document.getId(), savePolicy.getMaxImageCount());
                break;
            }
            byte[] imageBytes = decodeDoclingImage(document, image, imageIndex);
            if (imageBytes.length == 0) {
                continue;
            }
            if (savePolicy.isOversized(imageBytes.length)) {
                log.warn("Skip oversized Docling image. documentId={} imageIndex={} imageBytes={} maxImageBytes={}",
                        document.getId(), imageIndex, imageBytes.length, savePolicy.getMaxImageBytes());
                continue;
            }
            Long chunkId = documentImageChunkMatcher.matchChunkId(image, chunkPOs);
            saveDoclingImage(document, image, imageIndex, chunkId, imageBytes);
            savedCount++;
        }
    }

    private List<RagChunkPO> buildImageKnowledgeChunks(RagConfigDO config, RagPO knowledge, RagDocumentPO document,
                                                       DocumentParseResult parseResult, int startChunkIndex) {
        if (!shouldSaveImages(config)
                || parseResult == null
                || parseResult.getDoclingResult() == null) {
            return List.of();
        }
        return documentImageKnowledgeChunkBuilder.build(ImageKnowledgeChunkBuildRequest.builder()
                .ragId(knowledge.getId())
                .documentId(document.getId())
                .documentName(document.getName())
                .startChunkIndex(startChunkIndex)
                .images(parseResult.getDoclingResult().getImages())
                .build());
    }

    private byte[] decodeDoclingImage(RagDocumentPO document, DoclingParseResult.DoclingImage image, int imageIndex) {
        try {
            return Base64.getMimeDecoder().decode(image.getBase64());
        } catch (IllegalArgumentException e) {
            log.warn("Skip invalid Docling image base64. documentId={} imageIndex={}", document.getId(), imageIndex);
            return new byte[0];
        }
    }

    private void saveDoclingImage(RagDocumentPO document, DoclingParseResult.DoclingImage image, int imageIndex,
                                  Long chunkId, byte[] bytes) {
        try {
            // 复用资源模块，图片业务类型单独标记为 DOCUMENT_IMAGE，便于后续清理和审计。
            ResourcePO resource = resourceService.upload(
                    new ByteArrayInputStream(bytes),
                    "docling-" + document.getId() + "-" + imageIndex + ".png",
                    bytes.length,
                    ResourceBizTypeEnum.DOCUMENT_IMAGE.getValue(),
                    document.getId(),
                    null);
            ragDocumentImageMapper.insert(RagDocumentImagePO.builder()
                    .ragId(document.getRagId())
                    .documentId(document.getId())
                    .chunkId(chunkId)
                    .resourceId(resource.getId())
                    .imageIndex(imageIndex)
                    .imageUrl(resource.getAccessUrl())
                    .caption(image.getCaption())
                    .figureNo(image.getFigureNo())
                    .figureTitle(image.getFigureTitle())
                    .sectionTitle(image.getSectionTitle())
                    .sourcePage(image.getPage())
                    .documentName(document.getName())
                    .ocrText(image.getOcrText())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save Docling image. documentId={} imageIndex={}", document.getId(), imageIndex, e);
        }
    }

    private boolean shouldSaveImages(RagConfigDO config) {
        if (config == null || config.getParseParams() == null || config.getParseParams().getDocling() == null) {
            return true;
        }
        Boolean saveImages = config.getParseParams().getDocling().getSaveImages();
        return saveImages == null || Boolean.TRUE.equals(saveImages);
    }

    private void cleanExistingData(RagPO knowledge, Long documentId) {
        try {
            SnailAiVectorStore snailAiVectorStore = vectorStoreFactory.create(knowledge);
            String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", knowledge.getId()));
            List<RagChunkPO> existing = ragChunkMapper.selectList(
                    new LambdaQueryWrapper<RagChunkPO>()
                            .eq(RagChunkPO::getDocumentId, documentId)
                            .eq(RagChunkPO::getRagId, knowledge.getId())
                            .isNotNull(RagChunkPO::getVectorId));
            if (!existing.isEmpty()) {
                // 只删除没有被其他文档的 chunk 引用的向量
                List<String> vectorIdsToDelete = new ArrayList<>();
                for (RagChunkPO chunk : existing) {
                    if (StrUtil.isBlank(chunk.getVectorId())) {
                        continue;
                    }
                    Long refCount = ragChunkMapper.selectCount(
                            new LambdaQueryWrapper<RagChunkPO>()
                                    .eq(RagChunkPO::getRagId, knowledge.getId())
                                    .eq(RagChunkPO::getVectorId, chunk.getVectorId())
                                    .ne(RagChunkPO::getDocumentId, documentId));
                    if (refCount != 0) {
                        continue;
                    }
                    vectorIdsToDelete.add(chunk.getVectorId());
                }
                if (!vectorIdsToDelete.isEmpty()) {
                    snailAiVectorStore.delete(indexName, vectorIdsToDelete);
                    log.debug("RAG 清理文档旧向量 {} 个 documentId={}", vectorIdsToDelete.size(), documentId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean vector store for document: {}", documentId, e);
        }
        deleteFromSearchEngine(knowledge, documentId);
        ragChunkMapper.delete(new LambdaQueryWrapper<RagChunkPO>()
                .eq(RagChunkPO::getDocumentId, documentId)
                .eq(RagChunkPO::getRagId, knowledge.getId()));
        // 重处理同一文档前先删除旧图片，避免图片列表出现历史解析残留。
        cleanExistingImages(documentId);
    }

    private void cleanExistingImages(Long documentId) {
        // 先删资源文件，再删图片关系表；资源删除失败只记录日志，不阻断文档重处理。
        List<RagDocumentImagePO> images = ragDocumentImageMapper.selectList(
                new LambdaQueryWrapper<RagDocumentImagePO>().eq(RagDocumentImagePO::getDocumentId, documentId));
        for (RagDocumentImagePO image : images) {
            if (image.getResourceId() == null) {
                continue;
            }
            try {
                resourceService.delete(image.getResourceId());
            } catch (Exception e) {
                log.warn("Failed to delete Docling image resource. documentId={} resourceId={}",
                        documentId, image.getResourceId(), e);
            }
        }
        ragDocumentImageMapper.delete(
                new LambdaQueryWrapper<RagDocumentImagePO>().eq(RagDocumentImagePO::getDocumentId, documentId));
    }

    private void updateStatus(Long documentId, RagDocumentStatus status, String errorMsg) {
        RagDocumentPO update = new RagDocumentPO();
        update.setId(documentId);
        update.setStatus(status.getStatus());
        update.setErrorMsg(errorMsg);
        ragDocumentMapper.updateById(update);
    }

    private String resolveChunkSourceType(RagChunkPO chunk) {
        if (chunk == null || StrUtil.isBlank(chunk.getSourceType())) {
            return RagChunkSourceTypeEnum.TEXT.getValue();
        }
        return chunk.getSourceType();
    }

}
