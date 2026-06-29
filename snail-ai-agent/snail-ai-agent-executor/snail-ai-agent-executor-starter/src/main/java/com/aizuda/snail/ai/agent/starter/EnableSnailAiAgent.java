package com.aizuda.snail.ai.agent.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Snail AI Agent：标注在配置类上，导入 Agent 自动配置以开启智能体能力。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SnailAiAgentAutoConfiguration.class)
public @interface EnableSnailAiAgent {
}
