package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailCodeRequest(
        @NotBlank
        @Email
        @Size(max = 64)
        String email,

        @Size(max = 16)
        String verificationCode,

        // 空 = register；否则只允许三个已知场景（服务端还会再校验一次）
        @Pattern(regexp = "^(register|reset|update_email)?$")
        String scene
) {}
