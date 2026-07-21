package com.aizuda.snail.ai.agent.starter;

import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.agent.common.rpc.GrpcClientInvokeHandler;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.agent.common.rpc.GrpcChannelProvider;
import com.aizuda.snail.ai.agent.common.handler.ClientHeartbeatScheduler;
import com.aizuda.snail.ai.agent.common.rpc.ClientGrpcServer;
import com.aizuda.snail.ai.agent.core.ClientRequestDispatcher;
import com.aizuda.snail.ai.agent.core.advisor.InterceptorChainAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.StreamChunkForwarderAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.ThinkingCollectorAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.TokenUsageCollectorAdvisor;
import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.agent.core.executor.client.ChatClientFactory;
import com.aizuda.snail.ai.agent.core.executor.client.ChatClientCustomizer;
import com.aizuda.snail.ai.agent.core.executor.client.DefaultChatClientFactory;
import com.aizuda.snail.ai.agent.core.executor.model.ChatModelFactory;
import com.aizuda.snail.ai.agent.core.executor.prompt.DefaultPromptFactory;
import com.aizuda.snail.ai.agent.core.executor.prompt.PromptFactory;
import com.aizuda.snail.ai.agent.core.executor.tool.DefaultToolCallingManagerFactory;
import com.aizuda.snail.ai.agent.core.executor.tool.ToolCallingManagerFactory;
import com.aizuda.snail.ai.agent.core.executor.model.ClientChatModelSpecFactory;
import com.aizuda.snail.ai.agent.core.executor.model.ClientModelConfigValidator;
import com.aizuda.snail.ai.agent.core.executor.model.DefaultChatModelFactory;
import com.aizuda.snail.ai.agent.core.grpc.handler.ChatDispatchStreamingHandler;
import com.aizuda.snail.ai.agent.core.grpc.handler.ClientKickOutRequestHandler;
import com.aizuda.snail.ai.agent.core.grpc.handler.PingRequestHandler;
import com.aizuda.snail.ai.agent.core.interceptor.SnailAiInterceptor;
import com.aizuda.snail.ai.agent.core.interceptor.impl.LoggingInterceptor;
import com.aizuda.snail.ai.agent.core.resolver.*;
import com.aizuda.snail.ai.agent.core.runtime.ChatSessionRuntime;
import com.aizuda.snail.ai.agent.core.runtime.tool.ToolRuntime;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestDispatcher;
import com.aizuda.snail.ai.model.chat.ChatModelRuntime;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Hooks;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;

/**
 * Snail-AI Agent 客户端自动配置：装配对话执行链（ChatClient 工厂与各 Advisor）、
 * 工具解析器（Skill/RAG/基础工具）、服务端回调 RPC 代理，并在容器就绪后启动客户端 gRPC 服务与心跳。
 * 仅当 {@code snail-ai.enabled=true} 时生效；多数组件以 {@code @ConditionalOnMissingBean}
 * 暴露，宿主应用可按需替换默认实现。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2025-04-08
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SnailAiAgentProperties.class)
@ConditionalOnProperty(prefix = "snail-ai", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SnailAiAgentAutoConfiguration {

    private ClientHeartbeatScheduler heartbeatTask;
    private ClientGrpcServer clientGrpcServer;
    private GrpcChannelProvider grpcChannelProvider;

    // ==================== 核心组件 ====================

    @Bean
    public ActiveChatCounter activeChatCounter() {
        return new ActiveChatCounter();
    }


    @Bean
    public InterceptorChainAdvisor interceptorChainAdvisor(List<SnailAiInterceptor> interceptors) {
        return new InterceptorChainAdvisor(interceptors);
    }

    @Bean
    public ThinkingCollectorAdvisor thinkingCollectorAdvisor() {
        return new ThinkingCollectorAdvisor();
    }

    @Bean
    public TokenUsageCollectorAdvisor tokenUsageCollectorAdvisor() {
        return new TokenUsageCollectorAdvisor();
    }

    @Bean
    public StreamChunkForwarderAdvisor streamChunkForwarderAdvisor() {
        return new StreamChunkForwarderAdvisor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "snail-ai.agent", name = "logging-interceptor", havingValue = "true")
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientModelConfigValidator clientModelConfigValidator() {
        return new ClientModelConfigValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientChatModelSpecFactory clientChatModelSpecFactory(ClientModelConfigValidator validator) {
        return new ClientChatModelSpecFactory(validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatModelFactory chatModelFactory(ClientChatModelSpecFactory clientChatModelSpecFactory,
                                             ChatModelRuntime chatModelRuntime) {
        return new DefaultChatModelFactory(clientChatModelSpecFactory, chatModelRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptFactory promptFactory() {
        return new DefaultPromptFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolCallingManagerFactory toolCallingManagerFactory() {
        return new DefaultToolCallingManagerFactory();
    }

    /** ChatClient 工厂：默认 Advisor 按声明顺序生效（拦截器链 → 用量采集 → 思考采集 → 流分片转发），宿主可通过 ChatClientCustomizer 追加定制 */
    @Bean
    @ConditionalOnMissingBean
    public ChatClientFactory chatClientFactory(ChatModelFactory chatModelFactory,
                                               ToolCallingManagerFactory toolCallingManagerFactory,
                                               InterceptorChainAdvisor interceptorChainAdvisor,
                                               TokenUsageCollectorAdvisor tokenUsageCollectorAdvisor,
                                               ThinkingCollectorAdvisor thinkingCollectorAdvisor,
                                               StreamChunkForwarderAdvisor streamChunkForwarderAdvisor,
                                               ObjectProvider<ChatClientCustomizer> customizers) {
        return new DefaultChatClientFactory(DefaultChatClientFactory.DefaultChatClientFactoryConfig.builder()
                .chatModelFactory(chatModelFactory)
                .toolCallingManagerFactory(toolCallingManagerFactory)
                .defaultAdvisors(new org.springframework.ai.chat.client.advisor.api.Advisor[]{
                        interceptorChainAdvisor,
                        tokenUsageCollectorAdvisor,
                        thinkingCollectorAdvisor,
                        streamChunkForwarderAdvisor
                })
                .customizers(customizers.orderedStream().toList())
                .build());
    }

    @Bean
    public ClientChatExecutor clientChatExecutor(ChatClientFactory chatClientFactory,
                                                 PromptFactory promptFactory) {
        // 开启 Reactor 上下文自动传播：流式对话跨线程（虚拟线程/调度器切换）时保留 MDC 与会话上下文
        Hooks.enableAutomaticContextPropagation();
        return new ClientChatExecutor(ClientChatExecutor.ClientChatExecutorConfig.builder()
                .chatClientFactory(chatClientFactory)
                .promptFactory(promptFactory)
                .build());
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcChannelProvider grpcChannelProvider() {
        return new GrpcChannelProvider();
    }

    /** 服务端回调客户端：JDK 动态代理将 {@link RpcClient} 接口方法翻译为对服务端的 gRPC 调用（路由取方法上 @Mapping 注解的 path，带重试） */
    @Bean
    @ConditionalOnMissingBean
    public RpcClient serverCallback(
            GrpcChannelProvider channelProvider,
            SnailAiAgentProperties properties) {

        InvocationHandler handler = new GrpcClientInvokeHandler(channelProvider, properties);
        return (RpcClient) Proxy.newProxyInstance(
                RpcClient.class.getClassLoader(),
                new Class<?>[]{RpcClient.class},
                handler
        );
    }

    @Bean
    public ClientRagToolResolver clientRagToolResolver(RpcClient rpcClient) {
        return new ClientRagToolResolver(rpcClient);
    }

    @Bean
    public BaseToolResolver baseToolResolver(SnailAiAgentProperties props) {
        String shellTempDir = props.getShellTempDir();
        return new BaseToolResolver(shellTempDir);
    }

    @Bean
    public ClientMemoryToolResolver clientMemoryToolResolver() {
        return new ClientMemoryToolResolver();
    }

    @Bean
    public ClientSkillToolResolver clientSkillToolResolver(RpcClient rpcClient,
                                                           SnailAiAgentProperties props) {
        String skillTempDir = props.getSkillTempDir() != null ? props.getSkillTempDir() : "/tmp/snail-ai-agent/skills";
        return new ClientSkillToolResolver(rpcClient, skillTempDir);
    }

    @Bean
    public CustomToolCallbackProvider customToolCallbackProvider(ApplicationContext applicationContext) {
        return new CustomToolCallbackProvider(applicationContext);
    }

    @Bean
    public ToolRuntime toolRuntime(ClientSkillToolResolver skillToolResolver,
                                   ClientRagToolResolver ragToolResolver,
                                   BaseToolResolver baseToolResolver,
                                   CustomToolCallbackProvider customToolCallbackProvider) {
        return new ToolRuntime(skillToolResolver, ragToolResolver, baseToolResolver, customToolCallbackProvider);
    }

    @Bean
    public ChatSessionRuntime chatSessionRuntime(ClientChatExecutor clientChatExecutor,
                                                 ActiveChatCounter activeChatCounter,
                                                 ToolRuntime toolRuntime) {
        return new ChatSessionRuntime(clientChatExecutor, activeChatCounter, toolRuntime);
    }

    @Bean
    public PingRequestHandler pingRequestHandler(ActiveChatCounter activeChatCounter) {
        return new PingRequestHandler(activeChatCounter);
    }

    @Bean
    public ClientKickOutRequestHandler clientKickOutRequestHandler(ClientHeartbeatScheduler heartbeatScheduler) {
        return new ClientKickOutRequestHandler(heartbeatScheduler);
    }

    @Bean
    public ChatDispatchStreamingHandler chatDispatchStreamingHandler(ChatSessionRuntime chatSessionRuntime) {
        return new ChatDispatchStreamingHandler(chatSessionRuntime);
    }

    @Bean
    public ClientRequestDispatcher clientRequestDispatcher(GrpcRequestDispatcher grpcRequestDispatcher) {
        return new ClientRequestDispatcher(grpcRequestDispatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientHeartbeatScheduler clientHeartbeatScheduler(SnailAiAgentProperties props,
                                                             ActiveChatCounter counter,
                                                             GrpcChannelProvider grpcChannelProvider) {
        return new ClientHeartbeatScheduler(props, counter, grpcChannelProvider);
    }

    // ==================== 生命周期 ====================

    /**
     * 容器就绪后启动客户端 gRPC 服务与心跳调度。
     * gRPC 服务启动失败时仅记录错误并跳过心跳（应用可继续运行，但不会注册到服务端接收分发）。
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        SnailAiAgentProperties props = event.getApplicationContext().getBean(SnailAiAgentProperties.class);
        ClientRequestDispatcher dispatcher = event.getApplicationContext().getBean(ClientRequestDispatcher.class);
        grpcChannelProvider = event.getApplicationContext().getBean(GrpcChannelProvider.class);

        // 启动客户端 gRPC 服务
        clientGrpcServer = new ClientGrpcServer(
                props.getPort(),
                dispatcher.unaryHandler(),
                dispatcher.streamingHandler());
        try {
            clientGrpcServer.start();
        } catch (Exception e) {
            log.error("Failed to start client gRPC server on port {}", props.getPort(), e);
            return;
        }

        // 启动心跳
        heartbeatTask = event.getApplicationContext().getBean(ClientHeartbeatScheduler.class);
        heartbeatTask.start();

        log.info("Snail-AI Agent started: appId={}, grpcPort={}, server={}:{}",
                props.getAppId(), props.getPort(), props.getServer().getHost(), props.getServer().getPort());
    }

    /** 停机时先停心跳（摘除注册）再关 gRPC 服务，避免服务端继续向已关闭端口分发 */
    @PreDestroy
    public void shutdown() {
        if (heartbeatTask != null) {
            heartbeatTask.stop();
        }
        if (clientGrpcServer != null) {
            clientGrpcServer.stop();
        }
        log.info("Snail-AI Agent shutdown");
    }
}
