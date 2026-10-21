package com.taskflow.project.domain;

import java.time.Instant;

public record Project(
        Long id,
        Long ownerId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
