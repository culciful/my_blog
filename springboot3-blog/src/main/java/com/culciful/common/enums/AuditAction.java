package com.culciful.common.enums;

/**
 * 安全审计事件类型。存进 audit_log.action，也是 SLF4J 审计日志行里的事件名。
 */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    PASSWORD_RESET,
    ACCOUNT_DELETED
}
