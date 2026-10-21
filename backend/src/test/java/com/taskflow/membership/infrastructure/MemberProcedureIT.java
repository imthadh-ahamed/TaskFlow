package com.taskflow.membership.infrastructure;

import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for member stored procedures: V6 {@code remove_project_member}
 * and V9 add/find/updateRole procedures.
 *
 * <p>Requires a running PostgreSQL instance; skips gracefully otherwise.
 * Each test runs in a transaction that rolls back on completion.
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

    // --- AddProjectMember (V9) ---

    @Test
    void addMember_returnsMemberWithCorrectRole() {
        long projectId = projectRepository.save(ownerUserId, "Add Member Test", null).id();

        ProjectMember member = memberRepository.add(projectId, memberUserId, MemberRole.MEMBER);

        assertThat(member.userId()).isEqualTo(memberUserId);
        assertThat(member.projectId()).isEqualTo(projectId);
        assertThat(member.role()).isEqualTo(MemberRole.MEMBER);
        assertThat(member.createdAt()).isNotNull();
    }

    // --- GetMembersByProjectId (V9) ---

    @Test
    void findByProjectId_returnsOwnerAfterCreate() {
        long projectId = projectRepository.save(ownerUserId, "List Members Test", null).id();

        List<ProjectMember> members = memberRepository.findByProjectId(projectId);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).userId()).isEqualTo(ownerUserId);
        assertThat(members.get(0).role()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    void findByProjectId_includesAddedMember() {
        long projectId = projectRepository.save(ownerUserId, "Members List Test 2", null).id();
        memberRepository.add(projectId, memberUserId, MemberRole.VIEWER);

        List<ProjectMember> members = memberRepository.findByProjectId(projectId);

        assertThat(members).hasSize(2);
        assertThat(members).anyMatch(m -> m.userId() == memberUserId && m.role() == MemberRole.VIEWER);
    }

    // --- GetMemberByProjectAndUser (V9) ---

    @Test
    void findByProjectAndUser_returnsOwner() {
        long projectId = projectRepository.save(ownerUserId, "Find Member Test", null).id();

        Optional<ProjectMember> result = memberRepository.findByProjectAndUser(projectId, ownerUserId);

        assertThat(result).isPresent();
        assertThat(result.get().role()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    void findByProjectAndUser_returnsEmpty_whenNotMember() {
        long projectId = projectRepository.save(ownerUserId, "Non Member Test", null).id();

        Optional<ProjectMember> result = memberRepository.findByProjectAndUser(projectId, Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // --- UpdateMemberRole (V9) ---

    @Test
    void updateRole_changesRoleSuccessfully() {
        long projectId = projectRepository.save(ownerUserId, "Update Role Test", null).id();
        memberRepository.add(projectId, memberUserId, MemberRole.MEMBER);

        Optional<ProjectMember> updated = memberRepository.updateRole(projectId, memberUserId, MemberRole.VIEWER);

        assertThat(updated).isPresent();
        assertThat(updated.get().role()).isEqualTo(MemberRole.VIEWER);
    }
}
