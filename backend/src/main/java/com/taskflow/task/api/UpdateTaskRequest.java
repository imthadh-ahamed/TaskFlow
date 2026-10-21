package com.taskflow.task.api;

import com.taskflow.task.domain.TaskPriority;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/tasks/{taskId} body. All fields are optional.
 * - null title     → leave unchanged
 * - null description → leave unchanged; "" → clear to null
 * - null priority  → leave unchanged
 * - null assigneeId → leave unchanged; clearAssignee=true → remove assignee
 * - null dueDate   → leave unchanged; "" → clear to null
 */
public record UpdateTaskRequest(
        @Size(max = 255, message = "must not exceed 255 characters")
        String title,

        String description,

        TaskPriority priority,

        Long assigneeId,

        Boolean clearAssignee,

        String dueDate
) {}
