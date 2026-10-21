package com.taskflow.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEntry(
        long id,
        UUID eventId,
        String eventType,
        String payload,
        boolean delivered,
        int attempts,
        Instant createdAt
) {}
