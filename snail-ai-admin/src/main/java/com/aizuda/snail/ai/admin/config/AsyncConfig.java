package com.aizuda.snail.ai.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2025-07-20
 */
@Configuration
public class AsyncConfig  {

    @Bean(name = "snailAiAsyncExecutor")
    @Primary
    public Executor snailAiAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);              // 核心线程数
        executor.setMaxPoolSize(20);              // 最大线程数
        executor.setQueueCapacity(100);           // 队列容量
        executor.setKeepAliveSeconds(60);         // 线程空闲时间
        executor.setThreadNamePrefix("async-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
