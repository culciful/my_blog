package com.culciful.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        Long id,

        /** article id (RPC style: carried in body instead of the URL path) */
        Long aid,

        /** comment id, required for editComment */
        Long cid,

        Long authorId,

        @NotNull
        Boolean isMarkdown,

        /** 前端 content 为对象 {msg, member}；member 用于 @提及展示，后端暂不消费 */
        @NotNull
        @Valid
        Content content,

        Long parent,
        Long root
) {
    public record Content(
            @NotBlank
            @Size(max = 10_000)
            String msg
    ) {}

    /** 评论正文文本 */
    public String msg() {
        return content == null ? null : content.msg();
    }
}
