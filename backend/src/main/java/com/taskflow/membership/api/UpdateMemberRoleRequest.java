package com.taskflow.membership.api;

import com.taskflow.membership.domain.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "must not be null") MemberRole role
) {}
