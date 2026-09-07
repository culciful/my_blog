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
    SYSTEM_ERROR(-10013, "unknown system error"),
    /** 登录失败：不区分「用户不存在」与「密码错误」，避免用户枚举 */
    LOGIN_FAILED(-10014, "invalid username or password"),
    /** 评论已超过可编辑时间窗，或已有回复 */
    COMMENT_EDIT_LOCKED(-10015, "comment can no longer be edited"),
    /** 改密码时新密码与当前密码相同 */
    PASSWORD_NOT_CHANGED(-10016, "new password must be different from the current one");

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
