package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for user registration.
 */
public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Size(max = 64, message = "email length must be <= 64")
        @Pattern(
                regexp = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?",
                message = "email format is invalid"
        )
        String email,

        @NotBlank(message = "username is required")
        @Size(min = 1, max = 16, message = "username length must be between 1 and 16")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\u4E00-\\u9FA5~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-][a-zA-Z0-9\\u4E00-\\u9FA5 ~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{0,15}$",
                message = "username format is invalid"
        )
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 32, message = "password length must be between 6 and 32")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[A-Za-z\\d~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{6,32}$",
                message = "password format is invalid"
        )
        String password
) {}
