package com.aizuda.snail.ai.model.chat;

import com.aizuda.snail.ai.model.common.ModelAdapterException;
import com.aizuda.snail.ai.model.common.ModelErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;

/**
 * 对话模型运行时，按规格的 adapterKey 选择适配器并构建 {@link ChatModel}（含规格校验与异常归一化）。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
public class ChatModelRuntime {

    private final List<ChatModelAdapter> adapters;

    public ChatModelRuntime(List<ChatModelAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : adapters.stream()
                .sorted(Comparator.comparingInt(ChatModelRuntime::order))
                .toList();
    }

    /**
     * 按规格构建对话模型
     *
     * @param spec 对话模型构建规格
     * @return 对话模型实例
     */
    public ChatModel build(ChatModelSpec spec) {
        validate(spec);
        ChatModelAdapter adapter = adapters.stream()
                .filter(candidate -> spec.adapterKey().equalsIgnoreCase(candidate.adapterKey()))
                .findFirst()
                .orElseThrow(() -> new ModelAdapterException(
                        ModelErrorCode.ADAPTER_NOT_FOUND,
                        "No ChatModelAdapter for adapterKey=" + spec.adapterKey()));
        log.debug("ChatModel building via adapter={}, provider={}, model={}",
                adapter.adapterKey(), spec.providerKey(), spec.modelKey());
        try {
            return adapter.create(spec);
        } catch (ModelAdapterException e) {
            throw e;
        } catch (Exception e) {
            log.error("ChatModel build failed, adapter={}, provider={}, model={}",
                    adapter.adapterKey(), spec.providerKey(), spec.modelKey(), e);
            throw new ModelAdapterException(
                    ModelErrorCode.MODEL_BUILD_FAILED,
                    "ChatModel build failed: " + e.getMessage(), e);
        }
    }

    private static void validate(ChatModelSpec spec) {
        if (spec == null) {
            throw new ModelAdapterException(ModelErrorCode.INVALID_MODEL_CONFIG, "ChatModelSpec is required");
        }
        require(spec.adapterKey(), "adapterKey");
        require(spec.baseUrl(), "baseUrl");
        require(spec.apiKey(), "apiKey");
        require(spec.modelKey(), "modelKey");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ModelAdapterException(ModelErrorCode.INVALID_MODEL_CONFIG, field + " is required");
        }
    }

    private static int order(ChatModelAdapter adapter) {
        return adapter instanceof Ordered ordered ? ordered.getOrder() : Ordered.LOWEST_PRECEDENCE;
    }
}
