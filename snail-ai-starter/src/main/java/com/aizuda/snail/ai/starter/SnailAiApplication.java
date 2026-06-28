package com.aizuda.snail.ai.starter;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
