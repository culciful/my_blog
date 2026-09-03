package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 1, max = 20)
        // 禁止 @：避免用户名与他人邮箱字面相同造成登录歧义
        @Pattern(regexp = "^[a-zA-Z0-9\\u4E00-\\u9FA5~!#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-][a-zA-Z0-9\\u4E00-\\u9FA5 ~!#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{0,19}$")
        String username,

        @Email
        @Size(max = 64)
        String email,

        @Size(max = 16)
        String verificationCode,

        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[a-zA-Z])[A-Za-z\\d~!@#$%^&*()_+|}{\\[\\]\\\\/?><:\"`;.,'-]{8,64}$")
        String password
) {}
