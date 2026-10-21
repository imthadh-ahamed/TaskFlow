package com.taskflow.membership.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project membership details. `role` is one of OWNER, MEMBER, VIEWER.")
public record MemberResponse(long projectId, long userId, String role, String createdAt) {}
