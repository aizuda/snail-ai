package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentUploadRequestVO;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.features.rag.dedup.DedupResult;
import com.aizuda.snail.ai.features.rag.dedup.DocumentDedupChecker;
import com.aizuda.snail.ai.features.rag.dedup.UploadDecision;
import com.aizuda.snail.ai.features.rag.enums.DedupAction;
import com.aizuda.snail.ai.features.rag.enums.DedupStrategy;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.features.rag.strategy.importer.DocumentImportFactory;
import com.aizuda.snail.ai.features.rag.strategy.importer.DocumentImportStrategy;
import com.aizuda.snail.ai.features.rag.strategy.importer.ImportRequest;
import com.aizuda.snail.ai.features.rag.strategy.importer.ImportResult;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

/**
 * RAG 文档上传/导入服务，负责文件上传与 URL 导入的编排：导入策略选择、去重判定与决策执行
 * （新增 / 拒绝 / 跳过 / 覆盖）、原始文件落资源库以及文档行入库。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentUploadService {

    private static final String UPLOAD_LOCK_KEY_PREFIX = "rag:doc:upload:lock:";
    private static final long UPLOAD_LOCK_TIMEOUT_SECONDS = 30L;

    private final RagDocumentMapper ragDocumentMapper;
    private final RagMapper knowledgeMapper;
    private final DocumentImportFactory importFactory;
    private final ResourceService resourceService;
    private final DocumentDedupChecker dedupChecker;
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
            processOrchestrator.cleanupDocument(oldId);
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
     * 原始文件字节存在时落资源库，返回资源 ID（无字节内容时返回 null）。
     */
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

    /**
     * 构建去重冲突提示语（按命中类型区分同名 / 同内容 / 两者兼有）。
     */
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
        RagDocumentResponseVO vo = assembler.toResponseVO(po);
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
}
