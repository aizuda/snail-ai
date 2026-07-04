package com.aizuda.snail.ai.agent.chat.starter;

import com.aizuda.snail.ai.common.execption.BaseSnailAiException;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.util.JsonUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Set;

/**
 * Chat gateway exception handling.
 */
@Slf4j
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.agent.chat"})
@ResponseBody
public class SnailAiChatExceptionHandler {

    @ExceptionHandler(SnailAiAuthenticationException.class)
    public Object onAuthException(SnailAiAuthenticationException ex) {
        log.warn("Chat auth failed: {}", ex.getMessage());
        return response(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(BaseSnailAiException.class)
    public Object onBusinessException(BaseSnailAiException ex) {
        log.error("Chat business exception", ex);
        return response(0, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object onValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("Chat 参数校验失败: {}", message);
        return response(0, message);
    }

    @ExceptionHandler(BindException.class)
    public Object onBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("Chat 参数绑定校验失败: {}", message);
        return response(0, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object onConstraintViolationException(ConstraintViolationException ex) {
        String message = firstConstraintMessage(ex.getConstraintViolations());
        log.warn("Chat 参数校验失败: {}", message);
        return response(0, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Object onHandlerMethodValidationException(HandlerMethodValidationException ex) {
        Object[] arguments = ex.getDetailMessageArguments();
        String message = arguments != null && arguments.length > 0
                ? String.valueOf(arguments[0])
                : "参数校验失败";
        log.warn("Chat 参数校验失败: {}", message);
        return response(0, message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            TypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public Object onBadRequestException(Exception ex) {
        log.warn("Chat bad request: {}", ex.getMessage());
        return response(0, "参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    public Object onException(Exception ex) {
        log.error("Chat unexpected exception", ex);
        return response(0, "系统异常");
    }

    private Object response(int status, String message) {
        Result<String> result = Result.fail(status, message);
        if (isStreamingRequest()) {
            return JsonUtil.toJsonString(result);
        }
        return result;
    }

    private boolean isStreamingRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return false;
        }
        String accept = attrs.getRequest().getHeader("Accept");
        return accept != null && (accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)
                || accept.contains("application/x-ndjson"));
    }

    private String firstConstraintMessage(Set<ConstraintViolation<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return "参数校验失败";
        }
        return violations.iterator().next().getMessage();
    }
}
