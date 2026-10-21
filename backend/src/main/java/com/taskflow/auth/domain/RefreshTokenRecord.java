package com.taskflow.auth.domain;

import java.time.Instant;
import java.util.UUID;

/** Immutable snapshot of a refresh_tokens row. */
public record RefreshTokenRecord(
        UUID jti,
        UUID familyId,
        long userId,
        boolean revoked,
        UUID replacedByJti,
        Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
