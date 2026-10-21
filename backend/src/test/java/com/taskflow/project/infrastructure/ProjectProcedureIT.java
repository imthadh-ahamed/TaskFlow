package com.taskflow.project.infrastructure;

import com.taskflow.project.domain.Project;
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
 * Integration tests for the {@code create_project_with_owner} PostgreSQL procedure
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
class ProjectProcedureIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcProjectRepository projectRepository;

    private long testUserId;

    @BeforeAll
    static void checkDatabase(@Autowired DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            // connection succeeded — DB is reachable
        } catch (Exception e) {
            assumeTrue(false, "PostgreSQL unavailable — skipping integration tests: " + e.getMessage());
        }
    }

    @BeforeEach
    void insertTestUser() {
        // Runs inside the per-test @Transactional; rolled back after each test.
        testUserId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('proc-it-user@example.com', 'Proc IT User', '$2a$10$test') " +
                "RETURNING id",
                Long.class);
    }

    // --- create_project_with_owner ---

    @Test
    void createProject_returnsProjectWithCorrectFields() {
        Project project = projectRepository.save(testUserId, "IT Project", "desc");

        assertThat(project.id()).isPositive();
        assertThat(project.ownerId()).isEqualTo(testUserId);
        assertThat(project.name()).isEqualTo("IT Project");
        assertThat(project.description()).isEqualTo("desc");
        assertThat(project.createdAt()).isNotNull();
        assertThat(project.updatedAt()).isNotNull();
    }

    @Test
    void createProject_insertsOwnerMembership() {
        Project project = projectRepository.save(testUserId, "IT Project 2", null);

        Integer memberCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.project_members " +
                "WHERE project_id = ? AND user_id = ? AND role = 'owner'",
                Integer.class, project.id(), testUserId);
        assertThat(memberCount).isEqualTo(1);
    }

    @Test
    void createProject_nullDescription_isAccepted() {
        Project project = projectRepository.save(testUserId, "No Desc Project", null);

        assertThat(project.description()).isNull();
        assertThat(project.id()).isPositive();
    }

    @Test
    void createProject_throwsForNonExistentOwner() {
        long nonExistentUserId = Long.MAX_VALUE;

        assertThatExceptionOfType(DataAccessException.class)
                .isThrownBy(() -> projectRepository.save(nonExistentUserId, "Bad Project", null));
    }
}
