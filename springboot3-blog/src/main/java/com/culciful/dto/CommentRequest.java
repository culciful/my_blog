package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        Long id,
        Long authorId,

        @NotNull
        Boolean useMD,

        @NotBlank
        String content,

        Long parent,
        Long root
) {}
