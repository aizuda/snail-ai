package com.aizuda.snail.ai.agent.common.rpc;

import com.aizuda.snail.ai.agent.common.rpc.annotation.Mapping;
import com.aizuda.snail.ai.agent.common.rpc.annotation.Param;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.agent.common.exception.CallbackChannelUnavailableException;
import com.aizuda.snail.ai.agent.common.exception.CallbackException;
import com.aizuda.snail.ai.agent.common.exception.CallbackServerErrorException;
import com.aizuda.snail.ai.agent.common.exception.CallbackTimeoutException;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.client.GrpcChannelUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ServerCallback 动态代理核心处理器
 * <p>
 * 统一处理超时控制、重试逻辑、监控指标
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcClientInvokeHandler implements InvocationHandler {

    private final GrpcChannelProvider channelProvider;
    private final SnailAiAgentProperties properties;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取 @Mapping 注解
        Mapping mapping = method.getAnnotation(Mapping.class);
        if (mapping == null) {
            throw new UnsupportedOperationException("Method must have @Mapping annotation: " + method.getName());
        }

        String callbackName = method.getName();
        String uri = mapping.path();
        long timeout = mapping.timeout();

        // 执行调用（带重试和监控）
        return executeWithRetry(callbackName, uri, method, timeout, args, method.getReturnType());
    }

    /**
     * 执行调用（集成重试和监控）
     */
    private Object executeWithRetry(String callbackName, String uri, Method method,
                                    long timeout, Object[] args, Class<?> returnType) {

        SnailAiAgentProperties.ServerConfig server = properties.getServer();
        int maxAttempts = server.getRetryTimes();
        int retryInterval = server.getRetryInterval();

        Throwable lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Object result = doInvoke(uri, method, timeout, args, returnType);
                if (attempt > 1) {
                    log.info("Callback succeeded after {} attempts: {}", attempt, callbackName);
                }

                return result;

            } catch (CallbackChannelUnavailableException | StatusRuntimeException | CallbackTimeoutException e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    log.warn("Callback attempt #{} failed: {}, retrying in {}ms",
                            attempt, callbackName, retryInterval);

                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (CallbackServerErrorException e) {
                throw e;
            }
        }

        log.error("Callback failed after {} attempts: {}", maxAttempts, callbackName);
        throw new CallbackException("Callback failed after " + maxAttempts + " attempts: " + callbackName,
                lastException);
    }

    /**
     * 执行单次 gRPC 调用
     */
    private Object doInvoke(String uri, Method method, long timeout, Object[] args, Class<?> returnType) {
        // 1. 获取 channel（失败抛异常）
        ManagedChannel channel = channelProvider.getChannel();

        // 2. 构建请求参数
        Map<String, Object> params = buildParams(method, args);

        if (timeout <= 0) {
            timeout = properties.getServer().getTimeout();
        }

        // 3. 添加超时控制
        CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);

        // 4. 发送 gRPC 请求
        GrpcSnailAiResult result;
        try {
            result = GrpcChannelUtil.sendUnary(
                    channel, uri, JsonUtil.toJsonString(params),
                    channelProvider.getHeaders(), options
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new CallbackTimeoutException("Request timeout: " + uri, e);
            }
            throw e;
        }

        // 5. 检查响应状态
        if (result == null || result.getStatus() != 1) {
            String errorMsg = result != null ? result.getMessage() : "null response";
            log.warn("Server callback error: uri={}, error={}", uri, errorMsg);
            throw new CallbackServerErrorException("Server error: " + errorMsg);
        }

        // 6. 解析返回值
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }

        String data = result.getData();
        if (data.isEmpty() || "null".equals(data)) {
            return null;
        }

        return JsonUtil.parseObject(data, returnType);
    }

    /**
     * 构建参数 Map
     * <p>
     * 支持两种模式：
     * 1. DTO 对象：直接转为 Map（推荐）
     * 2. @Param 注解：按注解值构建 Map
     */
    private Map<String, Object> buildParams(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return Map.of();
        }

        // 如果只有一个参数且不是基本类型，视为 DTO 对象
        if (args.length == 1 && args[0] != null) {
            Object arg = args[0];

            // 如果是 Map，直接返回
            if (arg instanceof Map) {
                return (Map<String, Object>) arg;
            }

            // 如果是自定义对象（Request DTO），转为 Map
            Class<?> argClass = arg.getClass();
            if (!isSimpleType(argClass)) {
                // 使用 JsonUtil 将 DTO 转为 Map（保留字段名）
                String json = JsonUtil.toJsonString(arg);
                return JsonUtil.parseObject(json, Map.class);
            }
        }

        // 兜底：使用 @Param 注解构建参数 Map
        Map<String, Object> params = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (args[i] == null) {
                continue;
            }

            Param paramAnnotation = parameters[i].getAnnotation(Param.class);
            if (paramAnnotation != null) {
                params.put(paramAnnotation.value(), args[i]);
            } else {
                String paramName = parameters[i].getName();
                params.put(paramName, args[i]);
            }
        }

        return params;
    }

    /**
     * 判断是否为简单类型（基本类型及其包装类、String）
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Double.class
                || clazz == Float.class
                || clazz == Boolean.class
                || clazz == Short.class
                || clazz == Byte.class
                || clazz == Character.class;
    }
}
