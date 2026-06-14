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
