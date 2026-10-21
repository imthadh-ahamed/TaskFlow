package com.taskflow.auth.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final JdbcTemplate jdbc;

    JdbcRefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UUID jti, UUID familyId, long userId, Instant expiresAt) {
        jdbc.update(
                "INSERT INTO refresh_tokens (jti, family_id, user_id, expires_at) " +
                "VALUES (?, ?, ?, ?)",
                jti, familyId, userId, Timestamp.from(expiresAt)
        );
    }
}
