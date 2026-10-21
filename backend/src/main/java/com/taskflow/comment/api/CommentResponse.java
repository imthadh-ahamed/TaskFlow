package com.taskflow.comment.api;

public record CommentResponse(
        long id, long taskId, long authorId,
        String body, String createdAt, String updatedAt) {}
