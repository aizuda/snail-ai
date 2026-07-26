package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.dedup.DocumentDedupChecker;
import com.aizuda.snail.ai.features.rag.dedup.UploadDecision;
import com.aizuda.snail.ai.features.rag.enums.DedupAction;
import com.aizuda.snail.ai.features.rag.enums.DedupStrategy;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.admin.vo.PageResult;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库文档管理服务（门面），负责文档与切片的查询、切片增删改，并对外保持原有接口：
 * 上传/导入委托 {@link RagDocumentUploadService}，重新解析与文档/向量清理委托
 * {@link RagDocumentProcessOrchestrator}，视图转换委托 {@link RagDocumentAssembler}。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentService {

    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagMapper knowledgeMapper;
    private final RagDocumentUploadService uploadService;
    private final RagDocumentProcessOrchestrator processOrchestrator;
    private final RagDocumentAssembler assembler;

    /**
     * 通过 MultipartFile 上传文档（sourceType = UPLOAD）
     *
     * @param file    上传的文件
     * @param request 上传请求（知识库 ID、来源类型、去重策略等）
     * @return 文档响应（含去重决策结果）
     */
    public RagDocumentResponseVO upload(MultipartFile file, RagDocumentUploadRequestVO request) {
        return uploadService.upload(file, request);
    }

    /**
     * 从 URL 导入文档（sourceType = URL）
     *
     * @param request 上传请求（知识库 ID、URL、名称等）
     * @return 文档响应（含去重决策结果）
     */
    public RagDocumentResponseVO importFromUrl(RagDocumentUploadRequestVO request) {
        return uploadService.importFromUrl(request);
    }

    /**
     * 删除文档，同时清理其向量、资源文件与切片
     *
     * @param documentId 文档 ID
     */
    @Transactional
    public void delete(Long documentId) {
        processOrchestrator.cleanupDocument(documentId);
    }

    /**
     * 重新处理文档：状态重置为待处理并清空错误信息，触发重新解析/向量化
     *
     * @param documentId 文档 ID
     */
    public void reprocess(Long documentId) {
        processOrchestrator.reprocess(documentId);
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

        IPage<RagDocumentResponseVO> convert = page.convert(assembler::toResponseVO);
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
                .map(assembler::toImageResponseVO)
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
        // 按关键词模糊搜索：支持按 ID 或内容匹配
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            if (StrUtil.isNumeric(keyword)) {
                wrapper.and(w -> w
                        .eq(RagChunkPO::getId, Long.parseLong(keyword))
                        .or()
                        .like(RagChunkPO::getContent, keyword));
            } else {
                wrapper.like(RagChunkPO::getContent, keyword);
            }
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
                .map(po -> assembler.toChunkResponseVO(po, docNameMap.get(po.getDocumentId())))
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
        return assembler.toChunkResponseVO(chunk, documentNameOrNull(chunk.getDocumentId()));
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

        processOrchestrator.embedChunk(chunk, knowledge);

        updateDocumentChunkCount(request.getDocumentId());
        log.info("新增切片 chunkId={} documentId={} ragId={}", chunk.getId(), request.getDocumentId(), request.getRagId());

        RagChunkPO inserted = ragChunkMapper.selectById(chunk.getId());
        return assembler.toChunkResponseVO(inserted, documentNameOrNull(inserted.getDocumentId()));
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

        processOrchestrator.reEmbedChunk(chunk);

        log.info("更新切片 chunkId={}", chunkId);
        RagChunkPO updated = ragChunkMapper.selectById(chunkId);
        return assembler.toChunkResponseVO(updated, documentNameOrNull(updated.getDocumentId()));
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
        processOrchestrator.deleteChunkVector(chunk);
        ragChunkMapper.deleteById(chunkId);
        updateDocumentChunkCount(documentId);
        log.info("删除切片 chunkId={} documentId={}", chunkId, documentId);
    }

    /**
     * 直接落一行 RagDocument 并返回响应 VO，供常规 upload 与 preview-commit 两条链路复用，
     * 调用方自行准备好资源 ID。参数说明见 {@link RagDocumentUploadService#persistDocumentRow}。
     */
    public RagDocumentResponseVO persistDocumentRow(RagPO knowledge, String fileName,
                                                    String fileType, String sourceType,
                                                    String contentHash, Long resourceId,
                                                    UploadDecision decision) {
        return uploadService.persistDocumentRow(knowledge, fileName, fileType, sourceType,
                contentHash, resourceId, decision);
    }

    /**
     * 清理文档（公开版本）。preview-commit 流程的 OVERWRITE 直接调用，不再走 @Transactional 包装。
     *
     * @param documentId 文档 ID
     */
    public void cleanupDocument(Long documentId) {
        processOrchestrator.cleanupDocument(documentId);
    }

    /**
     * 暴露给 PreviewService 的去重判定器。
     *
     * @return 文档去重判定器
     */
    public DocumentDedupChecker getDedupChecker() {
        return uploadService.getDedupChecker();
    }

    /**
     * 解析生效的去重策略（请求优先，回退知识库配置）
     *
     * @param request   上传请求
     * @param knowledge 所属知识库
     * @return 生效的去重策略
     */
    public DedupStrategy resolveStrategy(RagDocumentUploadRequestVO request, RagPO knowledge) {
        return uploadService.resolveStrategy(request, knowledge);
    }

    /**
     * 解析生效的去重动作（请求优先，回退知识库配置）
     *
     * @param request   上传请求
     * @param knowledge 所属知识库
     * @return 生效的去重动作
     */
    public DedupAction resolveAction(RagDocumentUploadRequestVO request, RagPO knowledge) {
        return uploadService.resolveAction(request, knowledge);
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
        return uploadService.uploadLockKey(ragId);
    }

    /**
     * 获取上传锁超时时间（秒）
     *
     * @return 上传锁超时秒数
     */
    public long uploadLockTimeoutSeconds() {
        return uploadService.uploadLockTimeoutSeconds();
    }

    // ──────────────────────────────────── private ────────────────────────────────────

    private void updateDocumentChunkCount(Long documentId) {
        Long count = ragChunkMapper.selectCount(
                new LambdaQueryWrapper<RagChunkPO>().eq(RagChunkPO::getDocumentId, documentId));
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        if (doc != null) {
            doc.setChunkCount(count.intValue());
            ragDocumentMapper.updateById(doc);
        }
    }

    /** 查询切片所属文档名（文档不存在时返回 null）。 */
    private String documentNameOrNull(Long documentId) {
        RagDocumentPO doc = ragDocumentMapper.selectById(documentId);
        return doc != null ? doc.getName() : null;
    }

    private RagPO requireKnowledge(Long ragId) {
        RagPO knowledge = knowledgeMapper.selectById(ragId);
        if (knowledge == null) {
            throw new SnailAiException("Knowledge not found: " + ragId);
        }
        return knowledge;
    }
}
