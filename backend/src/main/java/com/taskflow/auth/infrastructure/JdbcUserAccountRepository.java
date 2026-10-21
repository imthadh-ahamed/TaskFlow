package com.taskflow.auth.infrastructure;

import com.taskflow.auth.domain.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JdbcUserAccountRepository implements UserAccountRepository {

    private final JdbcTemplate jdbc;

    JdbcUserAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<UserAccount> ROW_MAPPER = (rs, row) -> new UserAccount(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getBoolean("is_active")
    );

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jdbc.query(
                "SELECT id, email, display_name, password_hash, role, is_active " +
                "FROM users WHERE email = ?",
                ROW_MAPPER, email
        ).stream().findFirst();
    }

    @Override
    public UserAccount save(String email, String displayName, String passwordHash) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, display_name, password_hash) " +
                "VALUES (?, ?, ?) " +
                "RETURNING id, email, display_name, password_hash, role, is_active",
                ROW_MAPPER, email, displayName, passwordHash
        );
    }
}
