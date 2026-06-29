package com.aizuda.snail.ai.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通用 API 响应包装，统一返回状态码、提示消息与业务数据。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result<T> {

    /**
     * 状态码（1 成功，0 失败）
     */
    protected int status = 1;

    /**
     * 提示消息
     */
    protected String message;

    /**
     * 业务数据
     */
    protected T data;

    public Result() {
    }

    public Result(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Result(T data) {
        this.data = data;
    }

    public Result(String message, T data) {
        this.data = data;
        this.message = message;
    }

    public Result(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(1, "success", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(1, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(0, message, null);
    }

    /**
     * 失败响应（带状态码）
     */
    public static <T> Result<T> fail(int status, String message) {
        return new Result<>(status, message, null);
    }

}
