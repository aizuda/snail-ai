package com.aizuda.snail.ai.agent.starter;

import java.lang.annotation.*;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SnailAiAgentAutoConfiguration.class)
public @interface EnableSnailAiAgent {}
