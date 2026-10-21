package com.taskflow.task.api;

import com.taskflow.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** PATCH /api/v1/tasks/{taskId}/status body. */
public record UpdateTaskStatusRequest(
        @NotNull(message = "must not be null")
        TaskStatus status
) {}
