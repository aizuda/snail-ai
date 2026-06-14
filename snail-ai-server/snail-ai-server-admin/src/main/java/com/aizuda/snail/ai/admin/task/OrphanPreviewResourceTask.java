package com.aizuda.snail.ai.admin.task;

import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库上传预览孤儿资源清理任务
 * <p>
 * 当用户开了上传预览但未 commit 也未 cancel（关页面 / 网络中断），
 * Redis 里的 token 30 分钟后自然失效，但临时资源会变成孤儿。
 * 本任务每小时扫一次，把 bizType=DOCUMENT_PREVIEW 且超过 1 小时的资源彻底清理。
 *
 * @author opensnail
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanPreviewResourceTask {

    /** 至少存活 1 小时还在 PREVIEW 状态的资源视为孤儿（已超过 Redis token 30min TTL 的双倍） */
    private static final long ORPHAN_THRESHOLD_MINUTES = 60L;

    /** 单次扫描批量上限，防止积压时一次性拉太多 */
    private static final int BATCH_LIMIT = 200;

    private final ResourceMapper resourceMapper;
    private final ResourceService resourceService;

    /** 每小时整点 + 5 分钟（错开整点的高峰任务） */
    @Scheduled(cron = "0 5 * * * *")
    public void cleanupOrphanPreviewResources() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(ORPHAN_THRESHOLD_MINUTES);
            List<ResourcePO> orphans = resourceMapper.selectList(
                    new LambdaQueryWrapper<ResourcePO>()
                            .eq(ResourcePO::getBizType, ResourceBizTypeEnum.DOCUMENT_PREVIEW.getValue())
                            .lt(ResourcePO::getCreateDt, threshold)
                            .last("LIMIT " + BATCH_LIMIT));
            if (orphans.isEmpty()) {
                return;
            }
            int ok = 0;
            for (ResourcePO r : orphans) {
                try {
                    resourceService.delete(r.getId());
                    ok++;
                } catch (Exception e) {
                    log.warn("清理孤儿预览资源失败: id={}", r.getId(), e);
                }
            }
            log.info("清理孤儿预览资源完成：共 {} 条，成功 {} 条", orphans.size(), ok);
        } catch (Exception e) {
            log.warn("孤儿预览资源清理任务执行失败", e);
        }
    }
}
