package com.aizuda.snail.ai.agent.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * EnableSnailAiAgent
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
