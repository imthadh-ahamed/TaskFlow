package com.taskflow.project.infrastructure;

import com.taskflow.project.domain.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProjectRepository {

    private final JdbcTemplate jdbc;

    public JdbcProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates a project and its owner membership atomically by calling the
     * {@code create_project_with_owner} stored procedure (V6 migration).
     * The procedure inserts into {@code projects} and {@code project_members}
     * in a single PL/pgSQL block; if either insert fails the whole operation
     * rolls back.
     */
    public Project save(long ownerId, String name, String description) {
        return jdbc.execute((java.sql.Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.create_project_with_owner(?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, ownerId);
                cs.setString(2, name);
                if (description != null) {
                    cs.setString(3, description);
                } else {
                    cs.setNull(3, Types.VARCHAR);
                }
                // INOUT parameters: set initial null value, register as OUT.
                // PostgreSQL JDBC reports timestamptz as Types.TIMESTAMP (93) in CALL
                // statements, not as TIMESTAMP_WITH_TIMEZONE (2014).
                cs.setNull(4, Types.BIGINT);
                cs.registerOutParameter(4, Types.BIGINT);
                cs.setNull(5, Types.TIMESTAMP);
                cs.registerOutParameter(5, Types.TIMESTAMP);
                cs.setNull(6, Types.TIMESTAMP);
                cs.registerOutParameter(6, Types.TIMESTAMP);
                cs.execute();
                long id = cs.getLong(4);
                Timestamp createdAtTs = cs.getTimestamp(5);
                Timestamp updatedAtTs = cs.getTimestamp(6);
                return new Project(id, ownerId, name, description,
                        createdAtTs.toInstant(), updatedAtTs.toInstant());
            }
        });
    }

    public Optional<Project> findById(long id) {
        String sql = """
                SELECT id, owner_id, name, description, created_at, updated_at
                FROM projects
                WHERE id = ?
                """;
        return jdbc.query(sql, rowMapper(), id).stream().findFirst();
    }

    public List<Project> findAll(int limit, int offset) {
        String sql = """
                SELECT id, owner_id, name, description, created_at, updated_at
                FROM projects
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """;
        return jdbc.query(sql, rowMapper(), limit, offset);
    }

    public List<Project> findByMemberId(long userId, int limit, int offset) {
        String sql = """
                SELECT p.id, p.owner_id, p.name, p.description, p.created_at, p.updated_at
                FROM projects p
                JOIN project_members pm ON pm.project_id = p.id
                WHERE pm.user_id = ?
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT ? OFFSET ?
                """;
        return jdbc.query(sql, rowMapper(), userId, limit, offset);
    }

    public long countByMemberId(long userId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM projects p JOIN project_members pm ON pm.project_id = p.id WHERE pm.user_id = ?",
                Long.class, userId);
        return n != null ? n : 0L;
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM projects", Long.class);
        return n != null ? n : 0L;
    }

    public Optional<Project> update(long id, String name, String description) {
        String sql = """
                UPDATE projects
                SET name = ?, description = ?, updated_at = now()
                WHERE id = ?
                RETURNING id, owner_id, name, description, created_at, updated_at
                """;
        return jdbc.query(sql, rowMapper(), name, description, id).stream().findFirst();
    }

    public boolean deleteById(long id) {
        return jdbc.update("DELETE FROM projects WHERE id = ?", id) > 0;
    }

    private RowMapper<Project> rowMapper() {
        return (rs, rowNum) -> new Project(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
