package com.taskflow.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value object representing a notification event to be published
 * to Kafka via the transactional outbox (see OutboxRelayService).
 */
public record NotificationEvent(
        UUID eventId,
        String eventType,
        long projectId,
        long taskId,
        long actorId,
        Long assigneeId,
        Long previousAssigneeId,
        Instant occurredAt
) {
    public static NotificationEvent taskCreated(long projectId, long taskId,
                                                long createdBy, Long assigneeId) {
        return new NotificationEvent(
                UUID.randomUUID(), "task.created",
                projectId, taskId, createdBy, assigneeId, null, Instant.now());
    }

    public static NotificationEvent taskAssigned(long projectId, long taskId,
                                                  long assignedBy,
                                                  Long previousAssigneeId,
                                                  Long newAssigneeId) {
        return new NotificationEvent(
                UUID.randomUUID(), "task.assigned",
                projectId, taskId, assignedBy, newAssigneeId, previousAssigneeId, Instant.now());
    }
}
