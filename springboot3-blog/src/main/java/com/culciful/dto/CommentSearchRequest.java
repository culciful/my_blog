package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /comment/getComments} (RPC style).
 * {@code aid} and {@code root} are carried in the body instead of the URL.
 */
public record CommentSearchRequest(
        @NotNull
        Long aid,

        /** when present, list replies under this root comment; otherwise list root comments */
        Long root,

        Integer pageSize,
        Integer currentPage
) {
    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public int safeCurrentPage() {
        if (currentPage == null || currentPage < 1) {
            return 1;
        }
        return currentPage;
    }
}
