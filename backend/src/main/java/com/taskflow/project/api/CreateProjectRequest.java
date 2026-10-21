package com.taskflow.project.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * TODO: ownerId will be replaced by the authenticated user's ID once the auth
 * phase is implemented. Accepting it from the request body is a temporary
 * development-stage convenience only.
 */
public record CreateProjectRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must not exceed 200 characters")
        String name,

        String description,

        @NotNull(message = "must not be null")
        Long ownerId
) {}
