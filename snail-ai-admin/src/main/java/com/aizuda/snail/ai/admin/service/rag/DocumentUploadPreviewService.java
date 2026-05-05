package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.IdUtil;
import com.aizuda.snail.ai.admin.service.rag.preview.UploadPreviewState;
import com.aizuda.snail.ai.admin.service.rag.preview.UploadPreviewState.UploadPreviewItemState;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentUploadRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadCommitItemVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadCommitRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadCommitResultVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadPreviewItemVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadPreviewResultVO;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.rag.dedup.DedupResult;
import com.aizuda.snail.ai.features.rag.dedup.DocumentDedupChecker;
import com.aizuda.snail.ai.features.rag.dedup.UploadDecision;
import com.aizuda.snail.ai.features.rag.dedup.UploadDecisionType;
import com.aizuda.snail.ai.features.rag.enums.DedupAction;
import com.aizuda.snail.ai.features.rag.enums.DedupStrategy;
import com.aizuda.snail.ai.features.rag.enums.DocumentSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.util.ContentHashUtil;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库上传预览-提交两阶段流程
 * <p>
 * - preview：落临时资源、按策略生成每文件决策、token 写入内存
 * - commit：在知识库锁内重新校验决策（TOCTOU），按用户最终选择执行入库
 * - cancel：删除临时资源、失效 token
 *
 * @author opensnail
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadPreviewService {

    private static final long PREVIEW_TTL_MILLIS = 30L * 60L * 1000L; // 30分钟

    // 内存 Token 缓存：token -> {state, expiresAt}
    private final Map<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    private final RagDocumentService ragDocumentService;
    private final ResourceService resourceService;
    
    private static class TokenEntry {
        UploadPreviewState state;
        long expiresAt;

        TokenEntry(UploadPreviewState state) {
            this.state = state;
            this.expiresAt = System.currentTimeMillis() + PREVIEW_TTL_MILLIS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    // ──────────────────────────────────── preview ────────────────────────────────────

    public UploadPreviewResultVO preview(MultipartFile[] files, RagDocumentUploadRequestVO request) {
        if (files == null || files.length == 0) {
            throw new SnailAiCommonException("请选择要上传的文件");
        }
        RagPO knowledge = ragDocumentService.requireKnowledgeOrThrow(request.getRagId());
        DedupStrategy strategy = ragDocumentService.resolveStrategy(request, knowledge);
        DedupAction action = ragDocumentService.resolveAction(request, knowledge);
        DocumentDedupChecker checker = ragDocumentService.getDedupChecker();
        Long userId = currentUserIdOrNull();

        List<UploadPreviewItemState> stateItems = new ArrayList<>(files.length);
        List<UploadPreviewItemVO> voItems = new ArrayList<>(files.length);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String fileName = file.getOriginalFilename();
            String fileType = extractFileType(fileName);
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new SnailAiCommonException("读取上传文件失败：" + fileName);
            }
            String contentHash = ContentHashUtil.sha256Hex(bytes);

            ResourcePO resource = resourceService.upload(
                    new ByteArrayInputStream(bytes),
                    fileName,
                    bytes.length,
                    ResourceBizTypeEnum.DOCUMENT_PREVIEW.getValue(),
                    knowledge.getId(),
                    userId
            );

            DedupResult dedup = checker.check(knowledge.getId(), fileName, contentHash, strategy);
            UploadDecision decision = checker.decide(dedup, action);

            stateItems.add(UploadPreviewItemState.builder()
                    .tempResourceId(resource.getId())
                    .fileName(fileName)
                    .fileType(fileType)
                    .sourceType(DocumentSourceTypeEnum.UPLOAD.getValue())
                    .fileSize((long) bytes.length)
                    .contentHash(contentHash)
                    .decision(decision.getType().name())
                    .matchType(decision.getMatchType().name())
                    .conflictDocumentId(decision.getConflict() != null ? decision.getConflict().getId() : null)
                    .build());

            voItems.add(UploadPreviewItemVO.builder()
                    .tempResourceId(resource.getId())
                    .fileName(fileName)
                    .fileType(fileType)
                    .fileSize((long) bytes.length)
                    .contentHash(contentHash)
                    .decision(decision.getType().name())
                    .matchType(decision.getMatchType().name())
                    .conflictDocumentId(decision.getConflict() != null ? decision.getConflict().getId() : null)
                    .conflictDocumentName(decision.getConflict() != null ? decision.getConflict().getName() : null)
                    .rejectReason(decision.getType() == UploadDecisionType.REJECT
                            ? buildConflictReason(decision) : null)
                    .build());
        }

        if (stateItems.isEmpty()) {
            throw new SnailAiCommonException("没有可处理的文件");
        }

        String token = IdUtil.fastSimpleUUID();
        UploadPreviewState state = UploadPreviewState.builder()
                .ragId(knowledge.getId())
                .userId(userId)
                .dedupStrategy(strategy.getCode())
                .dedupAction(action.getCode())
                .items(stateItems)
                .build();
        tokenCache.put(token, new TokenEntry(state));

        return UploadPreviewResultVO.builder()
                .previewToken(token)
                .ragId(knowledge.getId())
                .items(voItems)
                .build();
    }

    // ──────────────────────────────────── commit ────────────────────────────────────

    public UploadCommitResultVO commit(UploadCommitRequestVO request) {
        UploadPreviewState state = loadState(request.getPreviewToken());
        RagPO knowledge = ragDocumentService.requireKnowledgeOrThrow(state.getRagId());

        // 使用本地锁替代分布式锁 (单机场景下足够)
        synchronized (ragDocumentService.uploadLockKey(knowledge.getId())) {
            UploadCommitResultVO result = doCommit(knowledge, state, request);
            
            // 整批 commit 完成才删除 token，避免部分失败时用户无法重试
            if (Boolean.FALSE.equals(result.getConflictChanged())) {
                tokenCache.remove(request.getPreviewToken());
            }
            return result;
        }
    }

    private UploadCommitResultVO doCommit(RagPO knowledge, UploadPreviewState state,
                                          UploadCommitRequestVO request) {
        Map<Long, UploadPreviewItemState> stateByResource = new HashMap<>();
        for (UploadPreviewItemState item : state.getItems()) {
            stateByResource.put(item.getTempResourceId(), item);
        }
        DocumentDedupChecker checker = ragDocumentService.getDedupChecker();
        DedupStrategy strategy = DedupStrategy.fromCode(state.getDedupStrategy());

        boolean conflictChanged = false;
        List<RagDocumentResponseVO> committed = new ArrayList<>();

        for (UploadCommitItemVO ui : request.getItems()) {
            UploadPreviewItemState item = stateByResource.get(ui.getTempResourceId());
            if (item == null) {
                throw new SnailAiCommonException("预览数据已失效，请重新上传");
            }
            UploadDecisionType userChoice = parseDecision(ui.getDecision());

            // TOCTOU 重新判定
            DedupResult fresh = checker.check(knowledge.getId(), item.getFileName(),
                    item.getContentHash(), strategy);
            if (isConflictChanged(item, fresh, userChoice)) {
                conflictChanged = true;
                committed.add(buildConflictChangedResponse(item, fresh));
                continue;
            }

            committed.add(executeOne(knowledge, item, fresh, userChoice));
        }

        return UploadCommitResultVO.builder()
                .conflictChanged(conflictChanged)
                .items(committed)
                .build();
    }

    /**
     * 判断「最新冲突状态」是否与用户在预览时基于的状态发生变化。
     * 任一情况成立即视为冲突已变化，需用户重新确认。
     */
    private boolean isConflictChanged(UploadPreviewItemState item, DedupResult fresh,
                                      UploadDecisionType userChoice) {
        boolean previewHadConflict = item.getConflictDocumentId() != null;
        boolean freshHasConflict = fresh.isHit();

        // 冲突有无发生反转
        if (previewHadConflict != freshHasConflict) {
            return true;
        }
        // 用户选 NEW 但当前仍存在冲突
        if (userChoice == UploadDecisionType.NEW && freshHasConflict) {
            return true;
        }
        // 用户选 SKIP/OVERWRITE 但当前已无冲突
        if (userChoice != UploadDecisionType.NEW && !freshHasConflict) {
            return true;
        }
        // 仍有冲突但目标文档发生了变化
        if (freshHasConflict && fresh.getConflict() != null
                && !fresh.getConflict().getId().equals(item.getConflictDocumentId())) {
            return true;
        }
        return false;
    }

    private RagDocumentResponseVO executeOne(RagPO knowledge, UploadPreviewItemState item,
                                             DedupResult fresh, UploadDecisionType userChoice) {
        RagDocumentPO conflict = fresh.getConflict();
        UploadDecision decision = new UploadDecision(userChoice, fresh.getMatchType(), conflict);

        return switch (userChoice) {
            case NEW -> {
                resourceService.updateBizType(item.getTempResourceId(),
                        ResourceBizTypeEnum.DOCUMENT.getValue());
                yield ragDocumentService.persistDocumentRow(knowledge, item.getFileName(),
                        item.getFileType(), item.getSourceType(), item.getContentHash(),
                        item.getTempResourceId(), decision);
            }
            case SKIP -> {
                // 跳过：临时资源不再需要，直接删除
                resourceService.delete(item.getTempResourceId());
                yield buildSkipResponse(item, conflict);
            }
            case OVERWRITE -> {
                if (conflict != null) {
                    ragDocumentService.cleanupDocument(conflict.getId());
                }
                resourceService.updateBizType(item.getTempResourceId(),
                        ResourceBizTypeEnum.DOCUMENT.getValue());
                RagDocumentResponseVO vo = ragDocumentService.persistDocumentRow(knowledge,
                        item.getFileName(), item.getFileType(), item.getSourceType(),
                        item.getContentHash(), item.getTempResourceId(), decision);
                vo.setConflictDocumentId(conflict != null ? conflict.getId() : null);
                yield vo;
            }
            case REJECT -> throw new SnailAiCommonException("不能在 commit 时提交 REJECT 决策");
        };
    }

    // ──────────────────────────────────── cancel ────────────────────────────────────

    public void cancel(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        TokenEntry entry = tokenCache.remove(token);
        if (entry != null && entry.state != null && entry.state.getItems() != null) {
            for (UploadPreviewItemState item : entry.state.getItems()) {
                try {
                    resourceService.delete(item.getTempResourceId());
                } catch (Exception e) {
                    log.warn("Failed to delete preview resource: {}", item.getTempResourceId(), e);
                }
            }
        }
    }

    // ──────────────────────────────────── 定时清理过期 Token ────────────────────────────────────

    @Scheduled(fixedDelay = 60_000) // 每 1 分钟检查一次
    public void cleanupExpiredTokens() {
        Iterator<Map.Entry<String, TokenEntry>> iter = tokenCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TokenEntry> entry = iter.next();
            if (entry.getValue().isExpired()) {
                iter.remove();
                log.debug("Cleaned up expired preview token: {}", entry.getKey());
            }
        }
    }

    // ──────────────────────────────────── helpers ────────────────────────────────────

    private UploadPreviewState loadState(String token) {
        TokenEntry entry = tokenCache.get(token);
        if (entry == null || entry.isExpired()) {
            throw new SnailAiCommonException("预览数据已过期或不存在，请重新上传");
        }
        UploadPreviewState state = entry.state;
        if (state == null || state.getItems() == null || state.getItems().isEmpty()) {
            throw new SnailAiCommonException("预览数据无效，请重新上传");
        }
        return state;
    }

    private RagDocumentResponseVO buildSkipResponse(UploadPreviewItemState item, RagDocumentPO conflict) {
        return RagDocumentResponseVO.builder()
                .ragId(conflict != null ? conflict.getRagId() : null)
                .name(item.getFileName())
                .fileType(item.getFileType())
                .sourceType(item.getSourceType())
                .fileSize(item.getFileSize())
                .decision(UploadDecisionType.SKIP.name())
                .matchType(item.getMatchType())
                .conflictDocumentId(conflict != null ? conflict.getId() : null)
                .build();
    }

    private RagDocumentResponseVO buildConflictChangedResponse(UploadPreviewItemState item, DedupResult fresh) {
        return RagDocumentResponseVO.builder()
                .name(item.getFileName())
                .fileType(item.getFileType())
                .sourceType(item.getSourceType())
                .fileSize(item.getFileSize())
                .decision("CONFLICT_CHANGED")
                .matchType(fresh.getMatchType().name())
                .conflictDocumentId(fresh.getConflict() != null ? fresh.getConflict().getId() : null)
                .errorMsg("冲突状态已变化，请重新预览后再提交")
                .build();
    }

    private String buildConflictReason(UploadDecision decision) {
        RagDocumentPO conflict = decision.getConflict();
        String name = conflict == null ? "" : "：" + conflict.getName();
        return switch (decision.getMatchType()) {
            case BY_NAME -> "已存在同名文档" + name;
            case BY_CONTENT -> "已存在相同内容的文档" + name;
            case BOTH -> "已存在同名且同内容的文档" + name;
            case NONE -> "上传被拒绝" + name;
        };
    }

    private UploadDecisionType parseDecision(String value) {
        try {
            return UploadDecisionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new SnailAiCommonException("非法的决策类型：" + value);
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return UserSessionUtils.currentUserSession().getId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
