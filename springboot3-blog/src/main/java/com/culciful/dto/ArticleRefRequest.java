package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /article/deleteArticle} (RPC style): identifies one article.
 */
public record ArticleRefRequest(
        @NotNull
        Long aid
) {}
