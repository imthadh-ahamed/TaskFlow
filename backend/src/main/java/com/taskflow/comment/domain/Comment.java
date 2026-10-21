package com.taskflow.comment.domain;

import java.time.Instant;
import java.util.Objects;

public record Comment(long id, long taskId, long authorId, String body,
                      Instant createdAt, Instant updatedAt) {

    public static final int MAX_BODY_LENGTH = 5_000;

    public Comment {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        body = body.strip();
        if (body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "Comment body must not exceed " + MAX_BODY_LENGTH + " characters");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
