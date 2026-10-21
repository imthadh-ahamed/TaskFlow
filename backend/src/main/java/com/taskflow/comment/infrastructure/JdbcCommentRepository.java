package com.taskflow.comment.infrastructure;

import com.taskflow.comment.domain.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcCommentRepository {

    private final JdbcTemplate jdbc;

    public JdbcCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Comment save(long taskId, long authorId, String body) {
        String sql = """
                INSERT INTO comments (task_id, author_id, body)
                VALUES (?, ?, ?)
                RETURNING id, task_id, author_id, body, created_at, updated_at
                """;
        return jdbc.queryForObject(sql, rowMapper(), taskId, authorId, body);
    }

    public List<Comment> findByTaskId(long taskId, int limit, int offset) {
        String sql = """
                SELECT id, task_id, author_id, body, created_at, updated_at
                FROM comments
                WHERE task_id = ?
                ORDER BY created_at ASC
                LIMIT ? OFFSET ?
                """;
        return jdbc.query(sql, rowMapper(), taskId, limit, offset);
    }

    public long countByTaskId(long taskId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE task_id = ?", Long.class, taskId);
        return n != null ? n : 0L;
    }

    private RowMapper<Comment> rowMapper() {
        return (rs, rowNum) -> new Comment(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getLong("author_id"),
                rs.getString("body"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
