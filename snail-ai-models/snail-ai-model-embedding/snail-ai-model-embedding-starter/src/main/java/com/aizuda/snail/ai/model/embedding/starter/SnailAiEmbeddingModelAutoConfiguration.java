package com.aizuda.snail.ai.model.embedding.starter;

import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import com.aizuda.snail.ai.model.common.ModelCapability;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelAdapter;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelRuntime;
import com.aizuda.snail.ai.model.embedding.openai.OpenAiCompatibleEmbeddingModelAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 向量化模型自动配置，装配向量化模型运行时、OpenAI 兼容适配器及其描述符。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@AutoConfiguration
public class SnailAiEmbeddingModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModelRuntime embeddingModelRuntime(List<EmbeddingModelAdapter> adapters) {
        return new EmbeddingModelRuntime(adapters);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiCompatibleEmbeddingModelAdapter openAiCompatibleEmbeddingModelAdapter() {
        return new OpenAiCompatibleEmbeddingModelAdapter();
    }

    @Bean
    public ModelAdapterDescriptor openAiCompatibleEmbeddingAdapterDescriptor() {
        return ModelAdapterDescriptor.of(
                ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER,
                "OpenAI Compatible",
                ModelCapability.EMBEDDING);
    }
}
