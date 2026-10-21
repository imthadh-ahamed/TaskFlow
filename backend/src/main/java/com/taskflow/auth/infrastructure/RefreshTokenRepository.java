package com.taskflow.auth.infrastructure;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence for refresh token records.
 * Stores the jti (UUID token identifier) from the signed JWT — not the raw token.
 * Additional methods for rotation and revocation are added in T9.4.
 */
public interface RefreshTokenRepository {

    void save(UUID jti, UUID familyId, long userId, Instant expiresAt);
}
