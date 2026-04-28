package com.culciful.common.enums;

/**
 * 统一返回结果状态信息类
 */
public enum ResultCodeEnum {

    SUCCESS(0, "success"),
    USERNAME_ERROR(-501, "username error"),
    PASSWORD_ERROR(-503, "password error"),
    NOT_LOGIN(-504, "not login"),
    USERNAME_USED(-505, "username used"),
    SYSTEM_ERROR(-100000, "系统未知异常"),
    PARAM_ERROR(-100001, "参数格式校验失败");

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
