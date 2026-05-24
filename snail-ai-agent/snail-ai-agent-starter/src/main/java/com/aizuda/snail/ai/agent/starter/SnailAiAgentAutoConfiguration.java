package com.aizuda.snail.ai.agent.starter;

import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.agent.common.rpc.GrpcClientInvokeHandler;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.agent.common.context.AgentChatContextThreadLocalAccessor;
import com.aizuda.snail.ai.agent.common.rpc.GrpcChannelProvider;
import com.aizuda.snail.ai.agent.common.handler.ClientHeartbeatScheduler;
import com.aizuda.snail.ai.agent.common.rpc.ClientGrpcServer;
import com.aizuda.snail.ai.agent.core.ClientRequestDispatcher;
import com.aizuda.snail.ai.agent.core.advisor.InterceptorChainAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.MemoryInjectionAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.StreamChunkForwarderAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.ThinkingCollectorAdvisor;
import com.aizuda.snail.ai.agent.core.advisor.TokenUsageCollectorAdvisor;
import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.agent.core.grpc.handler.ChatDispatchStreamingHandler;
import com.aizuda.snail.ai.agent.core.grpc.handler.PingRequestHandler;
import com.aizuda.snail.ai.agent.core.interceptor.SnailAiInterceptor;
import com.aizuda.snail.ai.agent.core.interceptor.impl.LoggingInterceptor;
import com.aizuda.snail.ai.agent.core.resolver.ClientMemoryToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.ClientRagToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.ClientSkillToolResolver;
import com.aizuda.snail.ai.agent.core.resolver.CustomToolCallbackProvider;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestDispatcher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Hooks;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;

/**
 * Snail-AI Agent 客户端自动配置
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Slf4j
@Configuration
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
    public MemoryInjectionAdvisor memoryInjectionAdvisor() {
        return new MemoryInjectionAdvisor();
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
    public ClientChatExecutor clientChatExecutor(Environment env,
                                                 MemoryInjectionAdvisor memoryInjectionAdvisor,
                                                 InterceptorChainAdvisor interceptorChainAdvisor,
                                                 TokenUsageCollectorAdvisor tokenUsageCollectorAdvisor,
                                                 ThinkingCollectorAdvisor thinkingCollectorAdvisor,
                                                 StreamChunkForwarderAdvisor streamChunkForwarderAdvisor) {

        Hooks.enableAutomaticContextPropagation();
        return new ClientChatExecutor(null,
                resolveActiveProfile(env),
                memoryInjectionAdvisor,
                interceptorChainAdvisor,
                tokenUsageCollectorAdvisor,
                thinkingCollectorAdvisor,
                streamChunkForwarderAdvisor);
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcChannelProvider grpcChannelProvider() {
        return new GrpcChannelProvider();
    }

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
    public PingRequestHandler pingRequestHandler(ActiveChatCounter activeChatCounter) {
        return new PingRequestHandler(activeChatCounter);
    }

    @Bean
    public ChatDispatchStreamingHandler chatDispatchStreamingHandler(ClientChatExecutor clientChatExecutor,
                                                                     ActiveChatCounter activeChatCounter,
                                                                     ClientSkillToolResolver skillToolResolver,
                                                                     ClientRagToolResolver ragToolResolver,
                                                                     ClientMemoryToolResolver memoryToolResolver,
                                                                     CustomToolCallbackProvider customToolCallbackProvider) {
        return new ChatDispatchStreamingHandler(
                clientChatExecutor, activeChatCounter, skillToolResolver,
                ragToolResolver, memoryToolResolver, customToolCallbackProvider);
    }

    @Bean
    public ClientRequestDispatcher clientRequestDispatcher(GrpcRequestDispatcher grpcRequestDispatcher) {
        return new ClientRequestDispatcher(grpcRequestDispatcher);
    }

    // ==================== 生命周期 ====================

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
        ActiveChatCounter counter = event.getApplicationContext().getBean(ActiveChatCounter.class);
        heartbeatTask = new ClientHeartbeatScheduler(props, counter, grpcChannelProvider);
        heartbeatTask.start();

        log.info("Snail-AI Agent started: appId={}, grpcPort={}, server={}:{}",
                props.getAppId(), props.getPort(), props.getServer().getHost(), props.getServer().getPort());
    }

    @PreDestroy
    public void shutdown() {
        if (heartbeatTask != null) heartbeatTask.stop();
        if (clientGrpcServer != null) clientGrpcServer.stop();
        log.info("Snail-AI Agent shutdown");
    }

    // ==================== 工具方法 ====================

    /**
     * 从 Spring Environment 解析当前激活的 profile，用于可观测性 environment 字段
     */
    private static String resolveActiveProfile(org.springframework.core.env.Environment env) {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length > 0) {
            return profiles[0];
        }
        String[] defaults = env.getDefaultProfiles();
        if (defaults.length > 0) {
            return defaults[0];
        }
        return "default";
    }
}
