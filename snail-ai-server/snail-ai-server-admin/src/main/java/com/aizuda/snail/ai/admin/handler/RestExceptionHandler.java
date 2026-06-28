package com.aizuda.snail.ai.admin.handler;

import cn.hutool.core.collection.CollUtil;
import com.aizuda.snail.ai.common.execption.AbstractError;
import com.aizuda.snail.ai.common.execption.BaseSnailAiException;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.common.util.StreamUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @description: 400 统一异常处理
 * @author: byteblogs
 * @date: 2019/09/30 17:02
 */
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.admin"})
@Slf4j
@ResponseBody
public class RestExceptionHandler {
    //异常类型
    public static final String DELIMITER_TO = "@";
    public static final String DELIMITER_COLON = ":";

    private boolean isStreamingRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return false;
        }
        String accept = attrs.getRequest().getHeader("Accept");
        return accept != null && (accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)
                || accept.contains("application/x-ndjson"));
    }

    private Object streamingError(int status, String message) {
        return JsonUtil.toJsonString(new Result<String>(status, message));
    }

    /**
     * 业务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler({Exception.class})
    public Object onException(Exception ex) {
        log.error("Exception class onException,", ex);
        if (isStreamingRequest()) {
            return streamingError(0, "System exception");
        }
        return new Result<String>(0, "System exception");
    }

    /**
     * 业务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler({BaseSnailAiException.class})
    public Object onBusinessException(BaseSnailAiException ex) {
        log.error("Exception class businessException", ex);
        if (isStreamingRequest()) {
            int code = (ex instanceof SnailAiAuthenticationException auth) ? auth.getErrorCode() : 0;
            return streamingError(code, ex.getMessage());
        }
        if (ex instanceof final SnailAiAuthenticationException authenticationException) {
            return new Result<String>(authenticationException.getErrorCode(), ex.getMessage());
        }

        return new Result<String>(0, ex.getMessage());
    }

    /**
     * 400错误
     *
     * @param ex
     * @return
     */
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public Object requestNotReadable(HttpMessageNotReadableException ex) {
        log.error("Exception class HttpMessageNotReadableException,", ex);
        if (isStreamingRequest()) {
            return streamingError(0, AbstractError.PARAM_INCORRECT.toString());
        }
        return new Result<String>(0, AbstractError.PARAM_INCORRECT.toString());
    }

    /**
     * validation 异常处理
     *
     * @param e ConstraintViolationException
     * @return HttpResult
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Object onConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        String errorMessage = CollUtil.isNotEmpty(constraintViolations)
                ? StreamUtils.join(constraintViolations, ConstraintViolation::getMessage, ";")
                : e.getMessage();
        if (isStreamingRequest()) {
            return streamingError(0, errorMessage);
        }
        return new Result<String>(0, errorMessage);
    }

    /**
     * validation 异常处理
     *
     * @param e MethodArgumentNotValidException
     * @return HttpResult
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        if (result != null && result.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            List<FieldError> errors = result.getFieldErrors();
            if (CollUtil.isNotEmpty(errors)) {
                FieldError error = errors.get(0);
                String rejectedValue = Objects.toString(error.getRejectedValue(), "");
                String defMsg = error.getDefaultMessage();
                // 排除类上面的注解提示
                if (rejectedValue.contains(DELIMITER_TO)) {
                    // 自己去确定错误字段
                    sb.append(defMsg);
                } else {
                    if (DELIMITER_COLON.contains(defMsg)) {
                        sb.append(error.getField()).append(" ").append(defMsg);
                    } else {
                        sb.append(error.getField()).append(" ").append(defMsg).append(":").append(rejectedValue);
                    }
                }
            } else {
                String msg = result.getAllErrors().get(0).getDefaultMessage();
                sb.append(msg);
            }

            if (isStreamingRequest()) {
                return streamingError(0, sb.toString());
            }
            return new Result<String>(0, sb.toString());
        }

        return null;
    }

    /**
     * Contrller 参数检验错误
     *
     * @param e 异常对象
     * @return HttpResult
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Object onHandlerMethodValidationException(HandlerMethodValidationException e) {
        Object[] detailMessageArguments = e.getDetailMessageArguments();
        String message = (detailMessageArguments != null && detailMessageArguments.length > 0)
                ? detailMessageArguments[0].toString()
                : "Parameter validation failed";
        if (isStreamingRequest()) {
            return streamingError(0, message);
        }
        return new Result<String>(0, message);
    }

    /**
     * 400错误
     */
    @ExceptionHandler({TypeMismatchException.class})
    public Object requestTypeMismatch(TypeMismatchException ex) {
        log.error("Exception class TypeMismatchException {},", ex.getMessage());
        if (isStreamingRequest()) {
            return streamingError(0, AbstractError.PARAM_INCORRECT.toString());
        }
        return new Result<String>(0, AbstractError.PARAM_INCORRECT.toString());
    }

    /**
     * 400错误
     */
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public Object requestMissingServletRequest(MissingServletRequestParameterException ex) {
        log.error("Exception class MissingServletRequestParameterException {},", ex.getMessage());
        if (isStreamingRequest()) {
            return streamingError(0, AbstractError.PARAM_INCORRECT.toString());
        }
        return new Result<String>(0, AbstractError.PARAM_INCORRECT.toString());
    }

    /**
     * 405错误
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseBody
    public Object request405() {
        log.error(" Exception class HttpRequestMethodNotSupportedException");
        if (isStreamingRequest()) {
            return streamingError(0, AbstractError.PARAM_INCORRECT.toString());
        }
        return new Result<String>(0, AbstractError.PARAM_INCORRECT.toString());
    }

    /**
     * 415错误
     */
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    public Object request415(HttpMediaTypeNotSupportedException ex) {
        log.error("Exception class HttpMediaTypeNotSupportedException {}", ex.getMessage());
        if (isStreamingRequest()) {
            return streamingError(0, AbstractError.PARAM_INCORRECT.toString());
        }
        return new Result<String>(0, AbstractError.PARAM_INCORRECT.toString());
    }
}
