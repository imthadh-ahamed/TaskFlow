package com.taskflow.auth.infrastructure;

import com.taskflow.auth.domain.RefreshTokenRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for refresh token records.
 * Stores the jti (UUID token identifier) from the signed JWT — not the raw token.
 */
public interface RefreshTokenRepository {

    /** Inserts a new active token record. */
    void save(UUID jti, UUID familyId, long userId, Instant expiresAt);

    /**
     * Fetches a token record and holds a row-level lock for the duration of the transaction.
     * Used during rotation to prevent concurrent refresh races: only one caller wins the lock;
     * the second caller will observe the token already revoked and trigger reuse detection.
     */
    Optional<RefreshTokenRecord> findByJtiForUpdate(UUID jti);

    /**
     * Marks a token as revoked and records its replacement.
     * Called during successful rotation: oldJti is the consumed token, newJti is its successor.
     */
    void revokeAndReplace(UUID oldJti, UUID newJti);

    /**
     * Marks a token as revoked without a replacement (logout).
     */
    void revokeByJti(UUID jti);

    /**
     * Revokes every token in the given family.
     * Called on reuse detection to immediately terminate all sessions derived from the same login.
     */
    void revokeFamily(UUID familyId);
}
