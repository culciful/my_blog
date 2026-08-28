package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /user/deletePackage} (RPC style): identifies one package.
 */
public record PackageRefRequest(
        @NotNull
        Long id,

        @NotNull
        Long pid
) {}
