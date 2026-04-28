package com.culciful.common.api;

import com.culciful.common.enums.ResultCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 全局统一返回结果类
 */
public class R<T> {
    private Integer errorCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public R() {
    }

    protected static <T> R<T> build(T data) {
        R<T> res = new R<>();
        if (data != null) {
            res.setResult(data);
        }
        return res;
    }

    public static <T> R<T> build(T body, ResultCodeEnum resultCodeEnum) {
        R<T> res = build(body);
        res.setErrorCode(resultCodeEnum.getErrorCode());
        return res;
    }

    public static <T> R<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    public static <T> R<T> fail(ResultCodeEnum resultCodeEnum) {
        R<T> r = new R<>();
        r.setErrorCode(resultCodeEnum.getErrorCode());
        r.setMessage(resultCodeEnum.getMessage());
        return r;
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

    public void setResult(T data) {
        this.result = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
