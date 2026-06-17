package com.aizuda.snail.ai.agent.example.config;

import com.aizuda.snail.ai.agent.core.executor.client.ChatClientBuildContext;
import com.aizuda.snail.ai.agent.core.executor.client.ChatClientCustomizer;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * <p>
 * 自定义扩展点
 * </p>
 *
 * @author opensnail
 * @date 2026-06-17
 */
@Component
public class MyChatClientCustomizer implements ChatClientCustomizer {

    @Override
    public void customize(ChatClientBuildContext context) {
        context.getBuilder().defaultToolContext(new HashMap<>() {{
            put("userId", "111");
        }});
    }
}
