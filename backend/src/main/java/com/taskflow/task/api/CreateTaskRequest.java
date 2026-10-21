package com.taskflow.task.api;

import com.taskflow.task.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/projects/{projectId}/tasks body.
 * assigneeId and dueDate are optional. priority defaults to MEDIUM when null.
 * dueDate format: yyyy-MM-dd.
 */
public record CreateTaskRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must not exceed 255 characters")
        String title,

        String description,

        TaskPriority priority,

        Long assigneeId,

        String dueDate
) {}
