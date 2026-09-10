package com.culciful.common.api;

import com.culciful.common.enums.ResultCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Global unified API response wrapper.
 */
public class R<T> {
    private Integer errorCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public R() {
    }

    protected static <T> R<T> build(T body) {
        R<T> response = new R<>();
        if (body != null) {
            response.setResult(body);
        }
        return response;
    }

    public static <T> R<T> build(T body, ResultCodeEnum resultCodeEnum) {
        R<T> response = build(body);
        response.setErrorCode(resultCodeEnum.getErrorCode());
        return response;
    }

    public static <T> R<T> ok(T body) {
        return build(body, ResultCodeEnum.SUCCESS);
    }

    public static <T> R<T> fail(ResultCodeEnum resultCodeEnum) {
        R<T> response = new R<>();
        response.setErrorCode(resultCodeEnum.getErrorCode());
        response.setMessage(resultCodeEnum.getMessage());
        return response;
    }

    public R<T> errorCode(Integer errorCode) {
        this.setErrorCode(errorCode);
        return this;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
