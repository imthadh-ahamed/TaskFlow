package com.taskflow.membership.api;

import com.taskflow.membership.domain.MemberRole;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotNull(message = "must not be null") Long userId,
        @NotNull(message = "must not be null") MemberRole role
) {}
