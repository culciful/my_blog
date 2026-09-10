package com.culciful.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleRequest(
        Long id,

        /** article id (RPC style: carried in body instead of the URL path), required for editArticle */
        Long aid,

        @NotBlank
        @Size(max = 64)
        String title,

        @NotBlank
        @Size(max = 100_000)
        String content,

        Long createTime,

        @NotNull
        Long pid,

        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 30) String> tags
) {}
