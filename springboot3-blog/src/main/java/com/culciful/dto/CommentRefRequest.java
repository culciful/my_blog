package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /comment/deleteComment} (RPC style): identifies one comment.
 */
public record CommentRefRequest(
        @NotNull
        Long aid,

        @NotNull
        Long cid
) {}
