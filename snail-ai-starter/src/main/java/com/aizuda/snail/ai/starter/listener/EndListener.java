package com.aizuda.snail.ai.starter.listener;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.log.SnailAiLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关闭监听器
 *
 * @author: opensnail
 * @date : 2021-11-19 19:00
 */
@Component
@RequiredArgsConstructor
public class EndListener implements ApplicationListener<ContextClosedEvent> {
    private final List<Lifecycle> lifecycleList;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        SnailAiLog.LOCAL.info("snail-ai client about to shutdown v{}", 1);
        lifecycleList.forEach(Lifecycle::close);
        SnailAiLog.LOCAL.info("snail-ai client closed successfully v{}", 1);
    }
}
