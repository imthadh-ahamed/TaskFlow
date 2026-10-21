package com.taskflow.task.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Stable response DTO. status and priority are lowercase to match DB values. */
@Schema(description = "Task details. `status` and `priority` are lowercase strings "
        + "(e.g. \"todo\", \"in_progress\", \"high\") matching the database values.")
public record TaskResponse(
        Long id,
        Long projectId,
        Long assigneeId,
        /** Display name of the assignee; null when the task has no assignee. Populated via JOIN on list/get. */
        String assigneeName,
        String title,
        String description,
        String status,
        String priority,
        String dueDate,
        String createdAt,
        String updatedAt
) {}
