package com.aizuda.snail.ai.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "com.aizuda.snail.ai")
@EnableConfigurationProperties
@MapperScan("com.aizuda.snail.ai.persistence")
@EnableAsync
@SpringBootConfiguration
@EnableScheduling
public class SnailAiSpringbootApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(SnailAiSpringbootApplication.class, args);
    }
}
