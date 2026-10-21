package com.taskflow.project.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project details")
public record ProjectResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        @Schema(description = "ISO 8601 UTC creation timestamp") String createdAt,
        @Schema(description = "ISO 8601 UTC last-update timestamp") String updatedAt
) {}
