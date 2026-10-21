package com.taskflow.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 5000, message = "must not exceed 5000 characters")
        String body,

        // Pre-auth: caller supplies the author. Replaced by SecurityContext when auth is added.
        @NotNull(message = "must not be null") Long authorId
) {}
