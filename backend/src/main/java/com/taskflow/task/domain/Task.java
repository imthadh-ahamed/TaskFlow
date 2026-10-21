package com.taskflow.task.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Full task representation. Persistence fields (id, projectId, createdAt,
 * updatedAt) are null for tasks that have not yet been saved.
 * No persistence annotations — mapping is the infrastructure layer's job.
 */
public record Task(
        Long id,
        Long projectId,
        Long assigneeId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static final int MAX_TITLE_LENGTH = 255;

    public Task {
        Objects.requireNonNull(status, "Task status must not be null");
        Objects.requireNonNull(priority, "Task priority must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        title = title.strip();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Task title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }
    }

    /** Convenience factory for unsaved tasks with default status and priority. */
    public static Task create(String title) {
        return new Task(null, null, null, title, null,
                TaskStatus.TODO, TaskPriority.MEDIUM, null, null, null);
    }

    public Task transitionTo(TaskStatus next) {
        Objects.requireNonNull(next, "Target status must not be null");
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot transition task from " + status + " to " + next);
        }
        return new Task(id, projectId, assigneeId, title, description,
                next, priority, dueDate, createdAt, updatedAt);
    }
}
