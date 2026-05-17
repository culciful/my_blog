package com.culciful.common.enums;

/**
 * Unified API result status codes.
 */
public enum ResultCodeEnum {

    SUCCESS(0, "success"),
    NOT_LOGIN(-10004, "not login"),
    USERNAME_ERROR(-10005, "username error"),
    PASSWORD_ERROR(-10006, "password error"),
    USERNAME_USED(-10007, "username used"),
    EMAIL_USED(-10008, "email already used"),
    FORBIDDEN(-10009, "forbidden"),
    NOT_FOUND(-10010, "resource not found"),
    BUSINESS_ERROR(-10011, "business error"),
    PARAM_ERROR(-10012, "param validation failed"),
    SYSTEM_ERROR(-10013, "unknown system error");

    private final Integer errorCode;
    private final String message;

    ResultCodeEnum(Integer errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
