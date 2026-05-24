package com.aizuda.snail.ai.agent.core.resolver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 自动发现 Spring 容器中所有包含 @Tool 方法的 Bean，转为 ToolCallback 并缓存。
 * <p>
 * 用户只需声明 @Component + @Tool 方法，启动时即可被自动发现并注入到 Agent ChatClient。
 * <p>
 * 示例:
 * <pre>
 * &#064;Component
 * public class OrderTool {
 *     &#064;Tool(name = "query_order", description = "查询订单信息")
 *     public String queryOrder(&#064;ToolParam(description = "订单ID") String orderId) {
 *         return orderService.query(orderId);
 *     }
 * }
 * </pre>
 *
 * @author opensnail
 * @date 2026-05-24
 */
@Slf4j
public class CustomToolCallbackProvider implements ToolCallbackProvider, InitializingBean {

    private static final String INTERNAL_TOOL_PACKAGE = "com.aizuda.snail.ai.agent.core.tool";

    private final ApplicationContext applicationContext;
    private ToolCallback[] cachedCallbacks = new ToolCallback[0];

    public CustomToolCallbackProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        List<ToolCallback> allCallbacks = new ArrayList<>();

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);

                // 排除框架内部工具包
                if (bean.getClass().getPackageName().startsWith(INTERNAL_TOOL_PACKAGE)) {
                    continue;
                }

                if (hasToolMethods(bean)) {
                    ToolCallback[] callbacks = ToolCallbacks.from(bean);
                    allCallbacks.addAll(Arrays.asList(callbacks));
                    log.info("Discovered @Tool bean: [{}] with {} tool(s)", beanName, callbacks.length);
                }
            } catch (Exception e) {
                // Bean 可能是 lazy 或 prototype，忽略
                log.debug("Skip bean [{}] during tool scanning: {}", beanName, e.getMessage());
            }
        }

        this.cachedCallbacks = allCallbacks.toArray(new ToolCallback[0]);
        if (cachedCallbacks.length > 0) {
            log.info("Custom tool discovery complete: {} tool(s) cached", cachedCallbacks.length);
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return cachedCallbacks;
    }

    private boolean hasToolMethods(Object bean) {
        return Arrays.stream(bean.getClass().getMethods())
                .anyMatch(m -> m.isAnnotationPresent(Tool.class));
    }
}
