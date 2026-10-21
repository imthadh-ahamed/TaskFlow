package com.taskflow.auth.infrastructure;

import com.taskflow.auth.domain.RefreshTokenRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final JdbcTemplate jdbc;

    JdbcRefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<RefreshTokenRecord> ROW_MAPPER = (rs, row) -> new RefreshTokenRecord(
            UUID.fromString(rs.getString("jti")),
            UUID.fromString(rs.getString("family_id")),
            rs.getLong("user_id"),
            rs.getBoolean("revoked"),
            rs.getString("replaced_by_jti") != null
                    ? UUID.fromString(rs.getString("replaced_by_jti")) : null,
            rs.getTimestamp("expires_at").toInstant()
    );

    @Override
    public void save(UUID jti, UUID familyId, long userId, Instant expiresAt) {
        jdbc.update(
                "INSERT INTO refresh_tokens (jti, family_id, user_id, expires_at) VALUES (?, ?, ?, ?)",
                jti, familyId, userId, Timestamp.from(expiresAt)
        );
    }

    @Override
    public Optional<RefreshTokenRecord> findByJtiForUpdate(UUID jti) {
        return jdbc.query(
                "SELECT jti, family_id, user_id, revoked, replaced_by_jti, expires_at " +
                "FROM refresh_tokens WHERE jti = ? FOR UPDATE",
                ROW_MAPPER, jti
        ).stream().findFirst();
    }

    @Override
    public void revokeAndReplace(UUID oldJti, UUID newJti) {
        jdbc.update(
                "UPDATE refresh_tokens SET revoked = true, replaced_by_jti = ? WHERE jti = ?",
                newJti, oldJti
        );
    }

    @Override
    public void revokeByJti(UUID jti) {
        jdbc.update("UPDATE refresh_tokens SET revoked = true WHERE jti = ?", jti);
    }

    @Override
    public void revokeFamily(UUID familyId) {
        jdbc.update("UPDATE refresh_tokens SET revoked = true WHERE family_id = ?", familyId);
    }
}
