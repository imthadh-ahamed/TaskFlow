package com.taskflow.membership.infrastructure;

import com.taskflow.project.infrastructure.JdbcProjectRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for the {@code remove_project_member} PostgreSQL procedure
 * (V6 migration). Requires a running PostgreSQL instance. If unavailable the suite
 * skips gracefully via JUnit 5 assumptions in {@code @BeforeAll}.
 *
 * <p>Each test runs inside a transaction that is rolled back on completion,
 * so no persistent test data is left in the database.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class MemberProcedureIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcMemberRepository memberRepository;

    @Autowired
    JdbcProjectRepository projectRepository;

    private long ownerUserId;
    private long memberUserId;

    @BeforeAll
    static void checkDatabase(@Autowired DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            // connection succeeded — DB is reachable
        } catch (Exception e) {
            assumeTrue(false, "PostgreSQL unavailable — skipping integration tests: " + e.getMessage());
        }
    }

    @BeforeEach
    void insertTestUsers() {
        // Runs inside the per-test @Transactional; rolled back after each test.
        ownerUserId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('member-it-owner@example.com', 'Owner User', '$2a$10$test') " +
                "RETURNING id",
                Long.class);

        memberUserId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('member-it-member@example.com', 'Member User', '$2a$10$test') " +
                "RETURNING id",
                Long.class);
    }

    // --- remove_project_member ---

    @Test
    void removeMember_deletesNonOwnerMember() {
        long projectId = projectRepository.save(ownerUserId, "Remove Test Project", null).id();

        jdbc.update(
                "INSERT INTO public.project_members (project_id, user_id, role) VALUES (?, ?, 'member')",
                projectId, memberUserId);

        boolean result = memberRepository.remove(projectId, memberUserId);

        assertThat(result).isTrue();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.project_members WHERE project_id = ? AND user_id = ?",
                Integer.class, projectId, memberUserId);
        assertThat(count).isZero();
    }

    @Test
    void removeMember_throwsWhenRemovingOwner() {
        long projectId = projectRepository.save(ownerUserId, "Owner Remove Test", null).id();

        // The procedure raises an exception that aborts the PostgreSQL transaction;
        // any query after this point in the same transaction would fail with
        // "current transaction is aborted". The thrown DataAccessException is the
        // definitive proof that the owner was not removed.
        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> memberRepository.remove(projectId, ownerUserId));
    }

    @Test
    void removeMember_throwsWhenMemberNotFound() {
        long projectId = projectRepository.save(ownerUserId, "Not Found Test", null).id();
        long nonMemberUserId = Long.MAX_VALUE;

        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> memberRepository.remove(projectId, nonMemberUserId));
    }
}
