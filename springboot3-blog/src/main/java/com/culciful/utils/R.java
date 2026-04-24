package com.culciful.utils;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 全局统一返回结果类
 */
public class R<T> {
    // 返回码
    private Integer errorCode;
    // 返回数据
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;
    // 错误时的信息
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public R() {}

    // 返回数据
    protected static <T> R<T> build(T data) {
        R<T> res = new R<>();
        if (data != null) res.setResult(data);
        return res;
    }

    public static <T> R<T> build(T body, ResultCodeEnum resultCodeEnum) {
        R<T> res = build(body);
        res.setErrorCode(resultCodeEnum.getErrorCode());
        return res;
    }

    /**
     * 成功
     */
    public static<T> R<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    /**
     * 失败
     */
    public static<T> R<T> fail(ResultCodeEnum resultCodeEnum) {
        R<T> r = new R<>();
        r.setErrorCode(resultCodeEnum.getErrorCode());
        r.setMessage(resultCodeEnum.getMessage());
        return r;
    }

    public R<T> errorCode(Integer errorCode){
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