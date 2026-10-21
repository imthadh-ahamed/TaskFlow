package com.taskflow.membership.api;

public record MemberResponse(long projectId, long userId, String role, String createdAt) {}
