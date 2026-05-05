package com.aizuda.snail.ai.admin.task;

import com.aizuda.snail.ai.common.enums.memory.MemoryStatusEnum;
import com.aizuda.snail.ai.persistence.memory.mapper.ConversationMemoryMapper;
import com.aizuda.snail.ai.persistence.memory.po.ConversationMemoryPO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 记忆过期定时任务
 * 每天凌晨 2:30 扫描并归档已过期的记忆（status → ARCHIVED）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryExpirationTask {

    private final ConversationMemoryMapper memoryMapper;

    @Scheduled(cron = "0 30 2 * * *")
    public void archiveExpiredMemories() {
        try {
            int rows = memoryMapper.update(null,
                    new LambdaUpdateWrapper<ConversationMemoryPO>()
                            .lt(ConversationMemoryPO::getExpiresAt, LocalDateTime.now())
                            .eq(ConversationMemoryPO::getStatus, MemoryStatusEnum.ACTIVE)
                            .set(ConversationMemoryPO::getStatus, MemoryStatusEnum.ARCHIVED));
            if (rows > 0) {
                log.info("记忆过期归档：共归档 {} 条", rows);
            }
        } catch (Exception e) {
            log.warn("记忆过期归档任务执行失败", e);
        }
    }
}
