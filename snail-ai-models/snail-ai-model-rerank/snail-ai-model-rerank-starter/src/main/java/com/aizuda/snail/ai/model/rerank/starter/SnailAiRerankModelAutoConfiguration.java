package com.aizuda.snail.ai.model.rerank.starter;

import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import com.aizuda.snail.ai.model.common.ModelCapability;
import com.aizuda.snail.ai.model.rerank.RerankModelAdapter;
import com.aizuda.snail.ai.model.rerank.RerankModelRuntime;
import com.aizuda.snail.ai.model.rerank.http.QwenRerankModelAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class SnailAiRerankModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RerankModelRuntime rerankModelRuntime(List<RerankModelAdapter> adapters) {
        return new RerankModelRuntime(adapters);
    }

    @Bean
    @ConditionalOnMissingBean
    public QwenRerankModelAdapter httpRerankModelAdapter() {
        return new QwenRerankModelAdapter();
    }

    @Bean
    public ModelAdapterDescriptor httpRerankAdapterDescriptor() {
        return ModelAdapterDescriptor.of(
                ModelAdapterDefaults.QWEN_RERANK,
                "Qwen Rerank",
                ModelCapability.RERANKER);
    }
}
