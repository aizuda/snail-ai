package com.aizuda.snail.ai.model.model.chat;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.model.Model;
import com.aizuda.snail.ai.common.mcp.McpServerRef;
import org.springframework.ai.tool.ToolCallback;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * author: opensnail
 * date: 2026-03-04
 */
public interface ChatModel extends Model {

    String chatModel(ChatModelDTO chatModelDTO) throws ModelCallException;

    void chatStreamModel(ChatStreamModelDTO chatModelDTO) throws ModelCallException;

    record ChatModelDTO(Long modelConfigId, String userContext, String systemContext) {
    }

    record ChatStreamModelDTO(Long modelConfigId, String userContext, String systemContext,
                        Consumer<String> messageConsumer, Runnable onComplete, Consumer<Throwable> onError) {
    }
}
