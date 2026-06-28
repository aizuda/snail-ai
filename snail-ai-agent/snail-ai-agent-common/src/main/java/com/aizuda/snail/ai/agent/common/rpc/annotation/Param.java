package com.aizuda.snail.ai.agent.common.rpc.annotation;

import java.lang.annotation.*;

/**
 * 回调方法参数注解
 * 用于标记参数名称，支持动态代理自动构建参数 Map
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {
    
    /**
     * 参数名称
     */
    String value();
}
