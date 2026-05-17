package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PackageRequest(
        @NotBlank
        @Size(max = 64)
        String pname
) {}
