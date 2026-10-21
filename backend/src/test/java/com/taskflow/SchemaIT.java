package com.taskflow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Schema-level integration tests. Verifies that:
 * <ul>
 *   <li>All Flyway migrations (V1–V11) have been applied and recorded in
 *       {@code flyway_schema_history}.</li>
 *   <li>Core uniqueness and foreign-key constraints are enforced by the
 *       database, not only by application code.</li>
 * </ul>
 *
 * <p>Requires a running PostgreSQL instance; skips gracefully otherwise.
 * Constraint-violation tests each run in their own transaction that rolls
 * back, so no persistent data is written.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class SchemaIT {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeAll
    static void checkDatabase(@Autowired DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            // connection succeeded — DB is reachable
        } catch (Exception e) {
            assumeTrue(false, "PostgreSQL unavailable — skipping integration tests: " + e.getMessage());
        }
    }

    // --- Migration coverage ---

    @Test
    void all_migrations_have_been_applied() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class);

        // V1–V11 must all be present
        assertThat(count).isGreaterThanOrEqualTo(11);
    }

    @Test
    void core_tables_exist() {
        // Each COUNT(*) returns 0 if the table exists and is empty; throws if it does not.
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.users", Integer.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.projects", Integer.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.project_members", Integer.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.tasks", Integer.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.comments", Integer.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM public.refresh_tokens", Integer.class))
                .doesNotThrowAnyException();
    }

    // --- Uniqueness constraints ---

    @Test
    void users_email_uniqueness_is_enforced() {
        jdbc.update(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('schema-it-unique@example.com', 'User A', '$2a$10$test')");

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbc.update(
                        "INSERT INTO public.users (email, display_name, password_hash) " +
                        "VALUES ('schema-it-unique@example.com', 'User B', '$2a$10$test')"));
    }

    @Test
    void project_members_composite_uniqueness_is_enforced() {
        long userId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('schema-it-member@example.com', 'Member', '$2a$10$test') RETURNING id",
                Long.class);

        long projectId = jdbc.queryForObject(
                "INSERT INTO public.projects (owner_id, name) VALUES (?, 'Schema IT Project') RETURNING id",
                Long.class, userId);

        // First insert (as owner from the project creation) uses raw SQL since
        // create_project_with_owner procedure also inserts the owner row; we insert
        // a non-owner row that we can then duplicate.
        jdbc.update(
                "INSERT INTO public.project_members (project_id, user_id, role) " +
                "VALUES (?, ?, 'member')",
                projectId, userId);

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbc.update(
                        "INSERT INTO public.project_members (project_id, user_id, role) " +
                        "VALUES (?, ?, 'viewer')",
                        projectId, userId));
    }

    // --- Foreign-key constraints ---

    @Test
    void tasks_require_valid_project_fk() {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbc.update(
                        "INSERT INTO public.tasks (project_id, title, status, priority) " +
                        "VALUES (?, 'Orphan task', 'todo', 'medium')",
                        Long.MAX_VALUE));
    }

    @Test
    void comments_require_valid_task_fk() {
        long userId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('schema-it-fk@example.com', 'FK User', '$2a$10$test') RETURNING id",
                Long.class);

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbc.update(
                        "INSERT INTO public.comments (task_id, author_id, body) " +
                        "VALUES (?, ?, 'Orphan comment')",
                        Long.MAX_VALUE, userId));
    }
}
