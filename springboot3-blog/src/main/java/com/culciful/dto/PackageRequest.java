package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PackageRequest(
        /** owner user id (RPC style: carried in body instead of the URL path) */
        Long id,

        /** package id, required for editPackage */
        Long pid,

        @NotBlank
        @Size(max = 64)
        String pname
) {}
