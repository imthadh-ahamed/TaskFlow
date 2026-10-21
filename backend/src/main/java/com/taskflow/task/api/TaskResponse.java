package com.taskflow.task.api;

/** Stable response DTO. status and priority are lowercase to match DB values. */
public record TaskResponse(
        Long id,
        Long projectId,
        Long assigneeId,
        String title,
        String description,
        String status,
        String priority,
        String dueDate,
        String createdAt,
        String updatedAt
) {}
