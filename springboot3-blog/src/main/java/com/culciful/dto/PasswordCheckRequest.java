package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 校验「当前密码」：不校验格式（比对现有凭证，不是设置新密码）。
 */
public record PasswordCheckRequest(
        @NotBlank
        @Size(max = 64)
        String password
) {}
