package com.taskflow.comment.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A comment posted on a task.")
public record CommentResponse(
        long id, long taskId, long authorId,
        String body, String createdAt, String updatedAt) {}
