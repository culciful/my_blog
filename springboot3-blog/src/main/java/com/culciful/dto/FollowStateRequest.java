package com.culciful.dto;

import jakarta.validation.constraints.NotNull;

public record FollowStateRequest(
        @NotNull
        Boolean value
) {}
