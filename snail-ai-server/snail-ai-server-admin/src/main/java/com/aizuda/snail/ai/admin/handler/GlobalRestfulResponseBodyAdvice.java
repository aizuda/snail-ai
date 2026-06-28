package com.aizuda.snail.ai.admin.handler;

import com.aizuda.snail.ai.admin.security.annotation.OriginalControllerReturnValue;
import com.aizuda.snail.ai.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.annotation.Annotation;

@Slf4j
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.admin"})
public class GlobalRestfulResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

    @Override
    public Object beforeBodyWrite(
            Object obj, MethodParameter methodParameter, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 流式响应不包装
        if (MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType)
                || NDJSON.isCompatibleWith(mediaType)) {
            return obj;
        }

        Annotation originalControllerReturnValue = methodParameter.getMethodAnnotation(OriginalControllerReturnValue.class);
        if (originalControllerReturnValue != null) {
            return obj;
        }

        if (obj instanceof Result) {
            return obj;
        } else {
            return new Result<>("Request succeeded", obj);
        }

    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

}
