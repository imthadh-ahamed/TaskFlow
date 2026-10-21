package com.taskflow.membership.infrastructure;

import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemberRepository {

    private final JdbcTemplate jdbc;

    public JdbcMemberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProjectMember add(long projectId, long userId, MemberRole role) {
        String sql = """
                INSERT INTO project_members (project_id, user_id, role)
                VALUES (?, ?, ?)
                RETURNING project_id, user_id, role, created_at
                """;
        return jdbc.queryForObject(sql, rowMapper(), projectId, userId, role.name().toLowerCase());
    }

    public List<ProjectMember> findByProjectId(long projectId) {
        String sql = """
                SELECT project_id, user_id, role, created_at
                FROM project_members
                WHERE project_id = ?
                ORDER BY created_at ASC
                """;
        return jdbc.query(sql, rowMapper(), projectId);
    }

    public Optional<ProjectMember> findByProjectAndUser(long projectId, long userId) {
        String sql = """
                SELECT project_id, user_id, role, created_at
                FROM project_members
                WHERE project_id = ? AND user_id = ?
                """;
        return jdbc.query(sql, rowMapper(), projectId, userId).stream().findFirst();
    }

    public Optional<ProjectMember> updateRole(long projectId, long userId, MemberRole role) {
        String sql = """
                UPDATE project_members SET role = ?
                WHERE project_id = ? AND user_id = ?
                RETURNING project_id, user_id, role, created_at
                """;
        return jdbc.query(sql, rowMapper(), role.name().toLowerCase(), projectId, userId)
                .stream().findFirst();
    }

    public boolean remove(long projectId, long userId) {
        return jdbc.update(
                "DELETE FROM project_members WHERE project_id = ? AND user_id = ?",
                projectId, userId) > 0;
    }

    private RowMapper<ProjectMember> rowMapper() {
        return (rs, rowNum) -> new ProjectMember(
                rs.getLong("project_id"),
                rs.getLong("user_id"),
                MemberRole.valueOf(rs.getString("role").toUpperCase()),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
