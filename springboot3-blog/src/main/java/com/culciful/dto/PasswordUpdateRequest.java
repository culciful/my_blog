package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 已登录用户改密码：需校验当前密码。
 */
public record PasswordUpdateRequest(
        @NotBlank
        @Size(max = 64)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 64, message = "password length must be between 8 and 64")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[A-Za-z\\d~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{8,64}$",
                message = "password format is invalid"
        )
        String newPassword
) {}
