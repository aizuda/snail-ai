package com.aizuda.snail.ai.common.model;

/**
 * 动态模型调用异常
 */
public class ModelCallException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    public ModelCallException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public ModelCallException(ErrorCode errorCode, String details) {
        super(errorCode.getMessage() + (details != null ? ": " + details : ""));
        this.errorCode = errorCode;
        this.details = details;
    }

    public ModelCallException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = cause.getMessage();
    }

    public ModelCallException(ErrorCode errorCode, String details, Throwable cause) {
        super(errorCode.getMessage() + (details != null ? ": " + details : ""), cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    /**
     * 错误代码枚举
     */
    public enum ErrorCode {
        CONFIG_NOT_FOUND("配置不存在"),
        CONFIG_DISABLED("配置已禁用"),
        ACCESS_DENIED("无权使用此配置"),
        PROVIDER_NOT_SUPPORTED("提供商暂不支持"),
        CHAT_MODEL_BUILD_FAILED("ChatModel构建失败"),
        CHAT_CLIENT_BUILD_FAILED("ChatClient构建失败"),
        API_KEY_DECRYPT_FAILED("APIKey解密失败"),
        MODEL_CALL_FAILED("模型调用失败"),
        INVALID_PARAMETER("参数非法"),
        CONFIG_PARSE_ERROR("配置解析错误");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
