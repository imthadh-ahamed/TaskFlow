package com.taskflow.task.infrastructure;

import com.taskflow.task.domain.Task;

/**
 * Repository projection that pairs a {@link Task} with the assignee's display name,
 * fetched in a single LEFT JOIN query to avoid N+1 lookups.
 *
 * <p>Used by {@link JdbcTaskRepository#findByProjectIdWithAssignee} and
 * {@link JdbcTaskRepository#findByIdWithAssignee} so callers never need a
 * per-task follow-up SELECT to resolve the assignee name.
 */
public record TaskWithAssignee(Task task, String assigneeName) {}
