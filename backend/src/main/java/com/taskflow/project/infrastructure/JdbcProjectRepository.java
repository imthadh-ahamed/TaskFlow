package com.taskflow.project.infrastructure;

import com.taskflow.project.domain.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProjectRepository {

    private final JdbcTemplate jdbc;

    public JdbcProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Project save(long ownerId, String name, String description) {
        String sql = """
                INSERT INTO projects (owner_id, name, description)
                VALUES (?, ?, ?)
                RETURNING id, owner_id, name, description, created_at, updated_at
                """;
        return jdbc.queryForObject(sql, rowMapper(), ownerId, name, description);
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
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
        return jdbc.query(sql, rowMapper(), limit, offset);
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
