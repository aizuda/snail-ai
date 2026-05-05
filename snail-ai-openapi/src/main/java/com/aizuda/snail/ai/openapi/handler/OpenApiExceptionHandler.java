package com.aizuda.snail.ai.openapi.handler;

import com.aizuda.snail.ai.common.execption.BaseSnailAiException;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * OpenAPI 统一异常处理
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.openapi"})
@ResponseBody
public class OpenApiExceptionHandler {

    @ExceptionHandler(SnailAiAuthenticationException.class)
    public Result<Void> onAuthException(SnailAiAuthenticationException ex) {
        log.warn("OpenAPI auth failed: {}", ex.getMessage());
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(BaseSnailAiException.class)
    public Result<Void> onBusinessException(BaseSnailAiException ex) {
        log.error("OpenAPI business exception", ex);
        return Result.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> onValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> onBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> onException(Exception ex) {
        log.error("OpenAPI unexpected exception", ex);
        return Result.fail("系统异常");
    }
}
