package com.aizuda.snail.ai.starter.listener;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.constants.SystemConstants;
import com.aizuda.snail.ai.common.log.SnailAiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.aizuda.snailjob.common.core.util.SnailAiVersion;

import java.util.List;

/**
 * 系统启动监听器
 *
 * @author: opensnail
 * @date : 2021-11-19 19:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartListener implements ApplicationListener<ContextRefreshedEvent> {
    private final List<Lifecycle> lifecycleList;
    private volatile boolean isStarted = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (isStarted) {
            SnailAiLog.LOCAL.info("snail-ai server already started v{}", 1);
            return;
        }
        System.out.println(MessageFormatter.format(SystemConstants.LOGO, SnailAiVersion.getVersion()).getMessage());
        SnailAiLog.LOCAL.info("snail-job server is preparing to start... v{}", SnailAiVersion.getVersion());
        lifecycleList.forEach(Lifecycle::start);
        SnailAiLog.LOCAL.info("snail-job server started successfully v{}", SnailAiVersion.getVersion());
        isStarted = true;
    }
}
