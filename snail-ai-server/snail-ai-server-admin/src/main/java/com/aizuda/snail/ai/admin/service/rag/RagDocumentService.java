package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.dedup.DedupResult;
import com.aizuda.snail.ai.features.rag.dedup.DocumentDedupChecker;
import com.aizuda.snail.ai.features.rag.dedup.UploadDecision;
import com.aizuda.snail.ai.features.rag.enums.DedupAction;
import com.aizuda.snail.ai.features.rag.enums.DedupStrategy;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkQueryVO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkUpdateRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentImageResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentQueryVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentUploadRequestVO;
import com.aizuda.snail.ai.features.rag.pipeline.DocumentPipeline;
import com.aizuda.snail.ai.features.rag.strategy.importer.DocumentImportFactory;
import com.aizuda.snail.ai.features.rag.strategy.importer.DocumentImportStrategy;
import com.aizuda.snail.ai.features.rag.strategy.importer.ImportRequest;
import com.aizuda.snail.ai.features.rag.strategy.importer.ImportResult;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import com.aizuda.snail.ai.vector.storage.vector.api.IndexNameBuilder;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识库文档管理服务，负责文档上传/去重/导入、文档与切片的增删改查及向量重建。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentService {

    private static final String DOWNLOAD_QUERY = "download=true";

    private static final String UPLOAD_LOCK_KEY_PREFIX = "rag:doc:upload:lock:";
    private static final long UPLOAD_LOCK_TIMEOUT_SECONDS = 30L;

    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagMapper knowledgeMapper;
    private final VectorStoreFactory vectorStoreFactory;
    private final DocumentPipeline documentPipeline;
    private final DocumentImportFactory importFactory;
    private final ResourceService resourceService;
    private final DocumentDedupChecker dedupChecker;

    /**
     * 通过 MultipartFile 上传文档（sourceType = UPLOAD）
     *
     * @param file    上传的文件
     * @param request 上传请求（知识库 ID、来源类型、去重策略等）
     * @return 文档响应（含去重决策结果）
     */
    public RagDocumentResponseVO upload(MultipartFile file, RagDocumentUploadRequestVO request) {
        RagPO knowledge = requireKnowledge(request.getRagId());

        String sourceType = StrUtil.isNotBlank(request.getSourceType()) ? request.getSourceType() : DocumentSourceTypeEnum.UPLOAD.getValue();
        DocumentImportStrategy strategy = importFactory.getStrategy(sourceType);

        ImportResult result = strategy.importDocument(ImportRequest.builder()
                .ragId(knowledge.getId())
                .file(file)
                .name(request.getName())
                .sourceType(sourceType)
                .build());

        return saveAndProcess(knowledge, result, request);
    }

    /**
     * 从 URL 导入文档（sourceType = URL）
     *
     * @param request 上传请求（知识库 ID、URL、名称等）
     * @return 文档响应（含去重决策结果）
     */
    public RagDocumentResponseVO importFromUrl(RagDocumentUploadRequestVO request) {
        RagPO knowledge = requireKnowledge(request.getRagId());

        DocumentImportStrategy strategy = importFactory.getStrategy("URL");
        ImportResult result = strategy.importDocument(ImportRequest.builder()
                .ragId(knowledge.getId())
                .url(request.getUrl())
                .name(request.getName())
                .sourceType(DocumentSourceTypeEnum.URL.getValue())
                .build());

        return saveAndProcess(knowledge, result, request);
    }


    /**
     * 删除文档，同时清理其向量、资源文件与切片
     *
     * @param documentId 文档 ID
     */
    @Transactional
    public void delete(Long documentId) {
        cleanupDocumentInternal(documentId);
    }

    /**
     * 清理文档及其相关资源（向量 / 资源库 / chunks / 文档行）。
     * 同时供普通删除与 OVERWRITE 上传链路复用。
     */
    private void cleanupDocumentInternal(Long documentId) {
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc == null) {
            return;
        }

        RagPO knowledge = knowledgeMapper.selectById(doc.getRagId());
        if (knowledge != null) {
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

    private void cleanDocumentImages(Long documentId) {
        // 文档删除或覆盖上传时同步清理 Docling 图片资源和图片关系表。
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
     * 获取文档持久化对象
     *
     * @param documentId 文档 ID
     * @return 文档 PO，不存在时返回 null
     */
    public RagDocumentPO getDocumentPO(Long documentId) {
        return ragDocumentMapper.selectById(documentId);
    }

    /**
     * 分页查询文档
     *
     * @param query 分页查询条件（知识库 ID、名称、状态等）
     * @return 文档分页结果
     */
    public PageResult<List<RagDocumentResponseVO>> page(RagDocumentQueryVO query) {
        LambdaQueryWrapper<RagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        if (query.getRagId() != null) {
            wrapper.eq(RagDocumentPO::getRagId, query.getRagId());
        }
        if (StrUtil.isNotBlank(query.getName())) {
            wrapper.like(RagDocumentPO::getName, query.getName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RagDocumentPO::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(RagDocumentPO::getCreateDt);

        PageDTO<RagDocumentPO> pageDTO = new PageDTO<>(query.getPage(), query.getSize());
        IPage<RagDocumentPO> page = ragDocumentMapper.selectPage(pageDTO, wrapper);

        IPage<RagDocumentResponseVO> convert = page.convert(this::toResponseVO);
        return new PageResult<>(pageDTO, convert.getRecords());
    }

    /**
     * 查询文档解析图片。
     *
     * @param documentId 文档 ID
     * @return Docling 解析图片列表
     */
    public List<RagDocumentImageResponseVO> listImages(Long documentId) {
        RagDocumentPO document = ragDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new SnailAiException("Document not found: " + documentId);
        }
        return ragDocumentImageMapper.selectList(new LambdaQueryWrapper<RagDocumentImagePO>()
                        .eq(RagDocumentImagePO::getDocumentId, documentId)
                        .orderByAsc(RagDocumentImagePO::getSourcePage)
                        .orderByAsc(RagDocumentImagePO::getId))
                .stream()
                .map(this::toImageResponseVO)
                .toList();
    }

    /**
     * 分页查询文档切片
     *
     * @param query 分页查询条件（文档 ID、知识库 ID 等）
     * @return 切片分页结果（含所属文档名）
     */
    public PageResult<List<RagChunkResponseVO>> chunkPage(RagChunkQueryVO query) {
        LambdaQueryWrapper<RagChunkPO> wrapper = new LambdaQueryWrapper<>();
        if (query.getDocumentId() != null) {
            wrapper.eq(RagChunkPO::getDocumentId, query.getDocumentId());
        }
        if (query.getRagId() != null) {
            wrapper.eq(RagChunkPO::getRagId, query.getRagId());
        }
        wrapper.orderByAsc(RagChunkPO::getParagraphIndex)
                .orderByAsc(RagChunkPO::getChunkIndex)
                .orderByDesc(RagChunkPO::getCreateDt);

        PageDTO<RagChunkPO> pageDTO = new PageDTO<>(query.getPage(), query.getSize());
        IPage<RagChunkPO> page = ragChunkMapper.selectPage(pageDTO, wrapper);
        List<RagChunkPO> records = page.getRecords();
        Set<Long> docIds = records.stream().map(RagChunkPO::getDocumentId).collect(Collectors.toSet());
        Map<Long, String> docNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(docIds)) {
            // 批量查询文档名，避免在循环中逐条 selectById 造成 N+1
            for (RagDocumentPO d : ragDocumentMapper.selectByIds(docIds)) {
                docNameMap.put(d.getId(), d.getName());
            }
        }
        List<RagChunkResponseVO> list = records.stream()
                .map(po -> toChunkResponseVO(po, docNameMap.get(po.getDocumentId())))
                .collect(Collectors.toList());
        return new PageResult<>(pageDTO, list);
    }

    /**
     * 获取切片详情
     *
     * @param chunkId 切片 ID
     * @return 切片详情（含所属文档名）
     */
    public RagChunkResponseVO getChunkDetail(Long chunkId) {
        RagChunkPO chunk = ragChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new SnailAiException("Chunk not found: " + chunkId);
        }
        String documentName = null;
        RagDocumentPO doc = ragDocumentMapper.selectById(chunk.getDocumentId());
        if (doc != null) {
            documentName = doc.getName();
        }
        return toChunkResponseVO(chunk, documentName);
    }

    /**
     * 新增文档切片并执行向量化
     *
     * @param request 切片创建请求（知识库 ID、文档 ID、内容）
     * @return 新增的切片详情
     */
    public RagChunkResponseVO createChunk(RagChunkCreateRequestVO request) {
        RagPO knowledge = requireKnowledge(request.getRagId());
        if (request.getDocumentId() == null) {
            throw new SnailAiException("documentId is required");
        }
        if (StrUtil.isBlank(request.getContent())) {
            throw new SnailAiException("content is required");
        }
        Integer maxIndex = ragChunkMapper.selectMaxChunkIndex(request.getDocumentId());
        int nextIndex = (maxIndex != null ? maxIndex : -1) + 1;

        RagChunkPO chunk = RagChunkPO.builder()
                .ragId(request.getRagId())
                .documentId(request.getDocumentId())
                .paragraphIndex(0)
                .chunkIndex(nextIndex)
                .content(request.getContent())
                .tokenCount(request.getContent() != null ? request.getContent().length() : 0)
                .build();
        ragChunkMapper.insert(chunk);

        try {
            documentPipeline.embedSingleChunk(chunk, knowledge);
        } catch (Exception e) {
            log.warn("新增切片向量化失败, chunkId={}", chunk.getId(), e);
        }

        updateDocumentChunkCount(request.getDocumentId());
        log.info("新增切片 chunkId={} documentId={} ragId={}", chunk.getId(), request.getDocumentId(), request.getRagId());

        RagChunkPO inserted = ragChunkMapper.selectById(chunk.getId());
        String documentName = null;
        RagDocumentPO doc = ragDocumentMapper.selectById(inserted.getDocumentId());
        if (doc != null) {
            documentName = doc.getName();
        }
        return toChunkResponseVO(inserted, documentName);
    }

    /**
     * 编辑文档切片内容并重新向量化
     *
     * @param chunkId 切片 ID
     * @param request 切片更新请求（新内容）
     * @return 更新后的切片详情
     */
    public RagChunkResponseVO updateChunk(Long chunkId, RagChunkUpdateRequestVO request) {
        RagChunkPO chunk = ragChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new SnailAiException("Chunk not found: " + chunkId);
        }
        if (StrUtil.isBlank(request.getContent())) {
            throw new SnailAiException("content is required");
        }
        chunk.setContent(request.getContent());
        chunk.setTokenCount(request.getContent().length());
        ragChunkMapper.updateById(chunk);

        RagPO knowledge = knowledgeMapper.selectById(chunk.getRagId());
        if (knowledge != null) {
            try {
                documentPipeline.reEmbedSingleChunk(chunk, knowledge);
            } catch (Exception e) {
                log.warn("编辑切片向量化失败, chunkId={}", chunkId, e);
            }
        }

        log.info("更新切片 chunkId={}", chunkId);
        RagChunkPO updated = ragChunkMapper.selectById(chunkId);
        String documentName = null;
        RagDocumentPO doc = ragDocumentMapper.selectById(updated.getDocumentId());
        if (doc != null) {
            documentName = doc.getName();
        }
        return toChunkResponseVO(updated, documentName);
    }

    private void updateDocumentChunkCount(Long documentId) {
        Long count = ragChunkMapper.selectCount(
                new LambdaQueryWrapper<RagChunkPO>().eq(RagChunkPO::getDocumentId, documentId));
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc != null) {
            doc.setChunkCount(count.intValue());
            ragDocumentMapper.updateById(doc);
        }
    }

    /**
     * 删除文档切片，同时清理其向量并刷新文档切片数
     *
     * @param chunkId 切片 ID
     */
    public void deleteChunk(Long chunkId) {
        RagChunkPO chunk = ragChunkMapper.selectById(chunkId);
        if (chunk == null) {
            return;
        }
        Long documentId = chunk.getDocumentId();
        if (StrUtil.isNotBlank(chunk.getVectorId())) {
            try {
                documentPipeline.deleteChunkVector(chunk);
            } catch (Exception e) {
                log.warn("Failed to delete vector for chunk: {}", chunkId, e);
            }
        }
        ragChunkMapper.deleteById(chunkId);
        updateDocumentChunkCount(documentId);
        log.info("删除切片 chunkId={} documentId={}", chunkId, documentId);
    }

    // ──────────────────────────────────── private ────────────────────────────────────

    /**
     * 上传链路核心：完成「判定 → 写入」两阶段（无分布式锁）。
     */
    private RagDocumentResponseVO saveAndProcess(RagPO knowledge, ImportResult result,
                                                 RagDocumentUploadRequestVO request) {
        return doSaveAndProcess(knowledge, result, request);
    }

    private RagDocumentResponseVO doSaveAndProcess(RagPO knowledge, ImportResult result,
                                                   RagDocumentUploadRequestVO request) {
        DedupStrategy strategy = effectiveStrategy(request, knowledge);
        DedupAction action = effectiveAction(request, knowledge);
        DedupResult dedup = dedupChecker.check(
                knowledge.getId(), result.getFileName(), result.getContentHash(), strategy);
        UploadDecision decision = dedupChecker.decide(dedup, action);

        return switch (decision.getType()) {
            case NEW -> insertNewDocument(knowledge, result, decision);
            case REJECT -> throw new SnailAiCommonException(buildConflictMessage(decision));
            case SKIP -> toResponseVOWithDecision(decision.getConflict(), decision);
            case OVERWRITE -> overwriteAndInsert(knowledge, result, decision);
        };
    }

    /**
     * 覆盖式上传：清理旧文档（含 chunks/向量/资源），按新文件重新入库。
     * conflictDocumentId 仍指向被替换的旧 ID，便于前端反馈。
     */
    private RagDocumentResponseVO overwriteAndInsert(RagPO knowledge, ImportResult result,
                                                     UploadDecision decision) {
        Long oldId = decision.getConflict() != null ? decision.getConflict().getId() : null;
        if (oldId != null) {
            cleanupDocumentInternal(oldId);
        }
        RagDocumentResponseVO vo = insertNewDocument(knowledge, result, decision);
        vo.setConflictDocumentId(oldId);
        return vo;
    }

    private RagDocumentResponseVO insertNewDocument(RagPO knowledge, ImportResult result,
                                                    UploadDecision decision) {
        Long resourceId = uploadResourceIfPresent(knowledge, result);
        return persistDocumentRow(knowledge, result.getFileName(), result.getFileType(),
                result.getSourceType(), result.getContentHash(), resourceId, decision);
    }

    /**
     * 直接落一行 RagDocument 并返回响应 VO。
     * 供常规 upload 与 preview-commit 两条链路复用，调用方自行准备好资源 ID。
     *
     * @param knowledge   所属知识库
     * @param fileName    文件名
     * @param fileType    文件类型
     * @param sourceType  来源类型
     * @param contentHash 内容哈希
     * @param resourceId  资源 ID（可为空）
     * @param decision    去重决策
     * @return 文档响应（含决策信息）
     */
    public RagDocumentResponseVO persistDocumentRow(RagPO knowledge, String fileName,
                                                    String fileType, String sourceType,
                                                    String contentHash, Long resourceId,
                                                    UploadDecision decision) {
        RagDocumentPO doc = RagDocumentPO.builder()
                .ragId(knowledge.getId())
                .name(fileName)
                .fileType(fileType)
                .sourceType(sourceType)
                .resourceId(resourceId)
                .contentHash(contentHash)
                .status(RagDocumentStatus.PENDING.getStatus())
                .chunkCount(0)
                .build();
        ragDocumentMapper.insert(doc);
        log.info("新增文档 documentId={} ragId={} name={}", doc.getId(), knowledge.getId(), fileName);
        return toResponseVOWithDecision(doc, decision);
    }

    /**
     * 清理文档（公开版本）。preview-commit 流程的 OVERWRITE 直接调用，不再走 @Transactional 包装。
     *
     * @param documentId 文档 ID
     */
    public void cleanupDocument(Long documentId) {
        cleanupDocumentInternal(documentId);
    }

    /**
     * 暴露给 PreviewService 的去重判定器。
     *
     * @return 文档去重判定器
     */
    public DocumentDedupChecker getDedupChecker() {
        return dedupChecker;
    }

    /**
     * 解析生效的去重策略（请求优先，回退知识库配置）
     *
     * @param request   上传请求
     * @param knowledge 所属知识库
     * @return 生效的去重策略
     */
    public DedupStrategy resolveStrategy(RagDocumentUploadRequestVO request, RagPO knowledge) {
        return effectiveStrategy(request, knowledge);
    }

    /**
     * 解析生效的去重动作（请求优先，回退知识库配置）
     *
     * @param request   上传请求
     * @param knowledge 所属知识库
     * @return 生效的去重动作
     */
    public DedupAction resolveAction(RagDocumentUploadRequestVO request, RagPO knowledge) {
        return effectiveAction(request, knowledge);
    }

    /**
     * 获取知识库，不存在时抛出异常
     *
     * @param ragId 知识库 ID
     * @return 知识库 PO
     */
    public RagPO requireKnowledgeOrThrow(Long ragId) {
        return requireKnowledge(ragId);
    }

    /**
     * 构建知识库上传分布式锁 key
     *
     * @param ragId 知识库 ID
     * @return 上传锁 key
     */
    public String uploadLockKey(Long ragId) {
        return UPLOAD_LOCK_KEY_PREFIX + ragId;
    }

    /**
     * 获取上传锁超时时间（秒）
     *
     * @return 上传锁超时秒数
     */
    public long uploadLockTimeoutSeconds() {
        return UPLOAD_LOCK_TIMEOUT_SECONDS;
    }

    private Long uploadResourceIfPresent(RagPO knowledge, ImportResult result) {
        if (result.getRawBytes() == null || result.getRawBytes().length == 0) {
            return null;
        }
        Long creatorId = null;
        try {
            creatorId = UserSessionUtils.currentUserSession().getId();
        } catch (Exception ignored) {
        }
        ResourcePO resource = resourceService.upload(
                new ByteArrayInputStream(result.getRawBytes()),
                result.getFileName(),
                result.getRawBytes().length,
                ResourceBizTypeEnum.DOCUMENT.getValue(),
                knowledge.getId(),
                creatorId
        );
        return resource.getId();
    }

    private DedupStrategy effectiveStrategy(RagDocumentUploadRequestVO request, RagPO knowledge) {
        if (request != null && request.getDedupStrategy() != null) {
            return DedupStrategy.fromCode(request.getDedupStrategy());
        }
        return DedupStrategy.fromCode(knowledge.getDedupStrategy());
    }

    private DedupAction effectiveAction(RagDocumentUploadRequestVO request, RagPO knowledge) {
        if (request != null && request.getDedupAction() != null) {
            return DedupAction.fromCode(request.getDedupAction());
        }
        return DedupAction.fromCode(knowledge.getDedupAction());
    }

    private String buildConflictMessage(UploadDecision decision) {
        RagDocumentPO conflict = decision.getConflict();
        String suffix = conflict == null ? "" : "：" + conflict.getName();
        return switch (decision.getMatchType()) {
            case BY_NAME -> "该知识库已存在同名文档" + suffix;
            case BY_CONTENT -> "该知识库已存在相同内容的文档" + suffix;
            case BOTH -> "该知识库已存在同名且同内容的文档" + suffix;
            case NONE -> "上传被拒绝" + suffix;
        };
    }

    private RagDocumentResponseVO toResponseVOWithDecision(RagDocumentPO po, UploadDecision decision) {
        RagDocumentResponseVO vo = toResponseVO(po);
        vo.setDecision(decision.getType().name());
        vo.setMatchType(decision.getMatchType().name());
        vo.setConflictDocumentId(decision.getConflict() != null ? decision.getConflict().getId() : null);
        return vo;
    }

    private RagPO requireKnowledge(Long ragId) {
        RagPO knowledge = knowledgeMapper.selectById(ragId);
        if (knowledge == null) {
            throw new SnailAiException("Knowledge not found: " + ragId);
        }
        return knowledge;
    }

    private RagDocumentResponseVO toResponseVO(RagDocumentPO po) {
        ResourcePO resource = resourceService.getById(po.getResourceId());
        String previewUrl = resource == null ? null : resource.getAccessUrl();

        return RagDocumentResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .name(po.getName())
                .fileType(po.getFileType())
                .sourceType(po.getSourceType())
                .status(po.getStatus())
                .errorMsg(po.getErrorMsg())
                .chunkCount(po.getChunkCount())
                .pageCount(po.getPageCount())
                .elementCount(po.getElementCount())
                .tableCount(po.getTableCount())
                .imageCount(po.getImageCount())
                .parseTime(po.getParseTime())
                .mdContent(po.getMdContent())
                .docMetadata(po.getDocMetadata())
                .fileSize(resource == null ? null : resource.getFileSize())
                .resourceId(po.getResourceId())
                .previewUrl(previewUrl)
                .downloadUrl(buildDownloadUrl(previewUrl))
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    private RagDocumentImageResponseVO toImageResponseVO(RagDocumentImagePO po) {
        return RagDocumentImageResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .documentId(po.getDocumentId())
                .chunkId(po.getChunkId())
                .resourceId(po.getResourceId())
                .imageIndex(po.getImageIndex())
                .imageUrl(po.getImageUrl())
                .caption(po.getCaption())
                .figureNo(po.getFigureNo())
                .figureTitle(po.getFigureTitle())
                .sectionTitle(po.getSectionTitle())
                .sourcePage(po.getSourcePage())
                .documentName(po.getDocumentName())
                .ocrText(po.getOcrText())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    private String buildDownloadUrl(String previewUrl) {
        if (StrUtil.isBlank(previewUrl)) {
            return null;
        }
        String separator = previewUrl.contains("?") ? "&" : "?";
        return previewUrl + separator + DOWNLOAD_QUERY;
    }

    private RagChunkResponseVO toChunkResponseVO(RagChunkPO po) {
        return toChunkResponseVO(po, null);
    }

    private RagChunkResponseVO toChunkResponseVO(RagChunkPO po, String documentName) {
        return RagChunkResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .documentId(po.getDocumentId())
                .paragraphIndex(po.getParagraphIndex())
                .chunkIndex(po.getChunkIndex())
                .content(po.getContent())
                .tokenCount(po.getTokenCount())
                .vectorId(po.getVectorId())
                .documentName(documentName)
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }
}
