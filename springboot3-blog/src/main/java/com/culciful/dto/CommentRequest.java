package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        Long id,

        /** article id (RPC style: carried in body instead of the URL path) */
        Long aid,

        /** comment id, required for editComment */
        Long cid,

        Long authorId,

        @NotNull
        Boolean useMD,

        @NotBlank
        String content,

        Long parent,
        Long root
) {}
