package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordCheckRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        @Pattern(regexp = "^(?=.*[a-zA-Z])[A-Za-z\\d~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{6,64}$")
        String password
) {}
