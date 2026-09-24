package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录：username 字段接受用户名或邮箱，不做严格格式校验（校验交给认证逻辑）。
 * 明文 JSON，传输安全依赖 HTTPS（生产硬性要求）。
 */
public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(min = 1, max = 64, message = "username length must be between 1 and 64")
        String username,

        // 登录不校验密码格式（校验交给认证逻辑），只限最大长度防超大 payload
        @NotBlank(message = "password is required")
        @Size(max = 64, message = "password too long")
        String password
) {}
