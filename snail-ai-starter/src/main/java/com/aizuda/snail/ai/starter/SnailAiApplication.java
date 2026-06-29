package com.aizuda.snail.ai.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * Snail AI 应用启动类，设置默认时区为 Asia/Shanghai 并启用异步与定时任务。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-06-28
 */
@EnableAsync
@EnableScheduling
@SpringBootConfiguration
@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = "com.aizuda.snail.ai")
public class SnailAiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(SnailAiApplication.class, args);
    }
}
