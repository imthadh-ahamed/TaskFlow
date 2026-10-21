package com.taskflow.project.api;

import jakarta.validation.constraints.Size;

/**
 * PATCH payload — all fields are optional. Null means "leave unchanged".
 * To clear description, send an empty string ("").
 * Name, if provided, must not be blank.
 */
public record UpdateProjectRequest(
        @Size(max = 200, message = "must not exceed 200 characters")
        String name,

        String description
) {}
