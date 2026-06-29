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
 * @since 2026-04-24
 */
@Slf4j
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.openapi"})
@ResponseBody
public class OpenApiExceptionHandler {

    /**
     * 处理认证异常
     *
     * @param ex 认证异常
     * @return 带错误码的失败响应
     */
    @ExceptionHandler(SnailAiAuthenticationException.class)
    public Result<Void> onAuthException(SnailAiAuthenticationException ex) {
        log.warn("OpenAPI auth failed: {}", ex.getMessage());
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理业务异常
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BaseSnailAiException.class)
    public Result<Void> onBusinessException(BaseSnailAiException ex) {
        log.error("OpenAPI business exception", ex);
        return Result.fail(ex.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param ex 参数校验异常
     * @return 含首个字段错误信息的失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> onValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("OpenAPI 参数校验失败: {}", message);
        return Result.fail(message);
    }

    /**
     * 处理表单/查询参数绑定校验异常
     *
     * @param ex 参数绑定异常
     * @return 含首个字段错误信息的失败响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> onBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("OpenAPI 参数绑定校验失败: {}", message);
        return Result.fail(message);
    }

    /**
     * 兜底处理未预期异常
     *
     * @param ex 未预期异常
     * @return 统一系统异常失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> onException(Exception ex) {
        log.error("OpenAPI unexpected exception", ex);
        return Result.fail("系统异常");
    }
}
