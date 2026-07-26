package com.aizuda.snail.ai.starter.listener;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.constants.SystemConstants;
import com.aizuda.snail.ai.common.log.SnailAiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.aizuda.snailjob.common.core.util.SnailAiVersion;

import java.util.List;

/**
 * 系统启动监听器
 *
 * @author opensnail
 * @since 2021-11-19 19:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartListener implements ApplicationListener<ContextRefreshedEvent> {
    private final List<Lifecycle> lifecycleList;
    private volatile boolean isStarted = false;

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (isStarted) {
            SnailAiLog.LOCAL.info("snail-ai server already started v{}", SnailAiVersion.getVersion());
            return;
        }
        // ASCII LOGO 直接写 stdout，避免日志框架为每行追加时间戳前缀破坏图形
        System.out.println(MessageFormatter.format(SystemConstants.LOGO, SnailAiVersion.getVersion()).getMessage());
        SnailAiLog.LOCAL.info("snail-ai server is preparing to start... v{}", SnailAiVersion.getVersion());
        lifecycleList.forEach(Lifecycle::start);
        SnailAiLog.LOCAL.info("snail-ai server started successfully v{}", SnailAiVersion.getVersion());
        isStarted = true;
    }
}
