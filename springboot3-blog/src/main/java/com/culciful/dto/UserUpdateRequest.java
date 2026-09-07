package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 已登录改资料：用户名 / 邮箱。改密码走 PasswordUpdateRequest。
 */
public record UserUpdateRequest(
        @Size(min = 1, max = 20)
        // 禁止 @：避免用户名与他人邮箱字面相同造成登录歧义
        @Pattern(regexp = "^[a-zA-Z0-9\\u4E00-\\u9FA5~!#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-][a-zA-Z0-9\\u4E00-\\u9FA5 ~!#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{0,19}$")
        String username,

        @Email
        @Size(max = 64)
        String email,

        @Size(max = 16)
        String verificationCode
) {}
