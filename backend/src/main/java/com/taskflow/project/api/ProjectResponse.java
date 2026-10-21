package com.taskflow.project.api;

public record ProjectResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String createdAt,
        String updatedAt
) {}
