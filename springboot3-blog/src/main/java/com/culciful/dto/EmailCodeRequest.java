package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailCodeRequest(
        @NotBlank
        @Email
        @Size(max = 64)
        String email,

        @Size(max = 16)
        String verificationCode,

        @Size(max = 32)
        String scene
) {}
