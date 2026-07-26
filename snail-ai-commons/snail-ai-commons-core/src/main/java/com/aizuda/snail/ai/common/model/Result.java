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
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 状态为成功的响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(1, "success", data);
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 提示消息
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 状态为成功的响应
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(1, message, data);
    }

    /**
     * 失败响应
     *
     * @param message 失败提示消息
     * @param <T>     业务数据类型
     * @return 状态为失败、数据为 null 的响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(0, message, null);
    }

    /**
     * 失败响应（带状态码）
     *
     * @param status  自定义状态码
     * @param message 失败提示消息
     * @param <T>     业务数据类型
     * @return 状态为指定值、数据为 null 的响应
     */
    public static <T> Result<T> fail(int status, String message) {
        return new Result<>(status, message, null);
    }

}
