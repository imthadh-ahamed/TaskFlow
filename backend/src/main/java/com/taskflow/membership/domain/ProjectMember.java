package com.taskflow.membership.domain;

import java.time.Instant;

public record ProjectMember(long projectId, long userId, MemberRole role, Instant createdAt) {}
