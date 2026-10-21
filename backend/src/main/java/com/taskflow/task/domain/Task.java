package com.taskflow.task.domain;

import java.util.Objects;

/**
 * Pure domain model for a task. No persistence annotations — mapping to/from
 * the database is the infrastructure layer's responsibility.
 */
public record Task(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority
) {

    static final int MAX_TITLE_LENGTH = 255;

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

    public static Task create(String title) {
        return new Task(title, null, TaskStatus.TODO, TaskPriority.MEDIUM);
    }

    public Task transitionTo(TaskStatus next) {
        Objects.requireNonNull(next, "Target status must not be null");
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot transition task from " + status + " to " + next);
        }
        return new Task(title, description, next, priority);
    }
}
