package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

public record FollowStateRequest(
        /** target user id (RPC style: carried in body instead of the URL path) */
        Long id,

        /** shouldFollow: true=关注, false=取关 */
        @NotNull
        Boolean shouldFollow
) {}
