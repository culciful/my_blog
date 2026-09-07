package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 匿名忘记密码：邮箱 + 验证码 + 新密码。
 */
public record PasswordResetRequest(
        @NotBlank
        @Email
        @Size(max = 64)
        String email,

        @NotBlank
        @Size(max = 16)
        String verificationCode,

        @NotBlank
        @Size(min = 8, max = 64, message = "password length must be between 8 and 64")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[A-Za-z\\d~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{8,64}$",
                message = "password format is invalid"
        )
        String newPassword
) {}
