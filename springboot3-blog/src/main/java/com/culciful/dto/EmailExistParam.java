package com.culciful.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Email existence request payload.
 */
public record EmailExistParam(
    @NotBlank
    @Email
    String email
) {}
