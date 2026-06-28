package com.aizuda.snail.ai.agent.common.rpc.annotation;

import java.lang.annotation.*;

/**
 * Server 回调方法映射注解
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mapping {
    
    /**
     * 回调路径 (如 /callback/skill/content)
     */
    String path();

    /**
     * 超时时间(ms)
     *
     * @return 5000ms
     */
    long timeout() default 0;
}
