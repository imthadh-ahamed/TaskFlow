package com.taskflow.comment.infrastructure;

import com.taskflow.comment.domain.Comment;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcCommentRepository {

    private final JdbcTemplate jdbc;

    public JdbcCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Comment save(long taskId, long authorId, String body) {
        return jdbc.execute((ConnectionCallback<Comment>) conn -> {
            callWithCursor(conn, "CALL public.SaveComment(?, ?, ?, ?)", ps -> {
                ps.setLong(1, taskId);
                ps.setLong(2, authorId);
                ps.setString(3, body);
            }, "save_comment_cursor");
            return fetchList(conn, "save_comment_cursor", rowMapper()).get(0);
        });
    }

    /**
     * Retrieves paginated comments via the GetCommentsTaskId stored procedure (V7).
     * The CALL and FETCH must run on the same connection within the same transaction;
     * CommentService.listComments is @Transactional(readOnly = true) to satisfy this.
     */
    public List<Comment> findByTaskId(long taskId, int limit, int offset) {
        if (limit <= 0 || offset < 0) {
            throw new IllegalArgumentException(
                    "Limit must be positive and offset must not be negative");
        }
        return jdbc.execute((ConnectionCallback<List<Comment>>) conn -> {
            callWithCursor(conn, "CALL public.GetCommentsTaskId(?, ?, ?, ?)", ps -> {
                ps.setLong(1, taskId);
                ps.setInt(2, limit);
                ps.setInt(3, offset);
            }, "comments_cursor");
            return fetchList(conn, "comments_cursor", rowMapper());
        });
    }

    public long countByTaskId(long taskId) {
        return jdbc.execute((ConnectionCallback<Long>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.CountCommentsByTaskId(?, ?)")) {
                cs.setLong(1, taskId);
                cs.setNull(2, Types.BIGINT);
                cs.registerOutParameter(2, Types.BIGINT);
                cs.execute();
                return cs.getLong(2);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private <T> List<T> fetchList(java.sql.Connection conn, String cursorName,
                                   RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> results = new ArrayList<>();
        try (Statement fetch = conn.createStatement();
             ResultSet rs = fetch.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
            while (rs.next()) {
                results.add(mapper.mapRow(rs, results.size()));
            }
        }
        try (Statement close = conn.createStatement()) {
            close.execute("CLOSE \"" + cursorName + "\"");
        }
        return results;
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private void callWithCursor(java.sql.Connection conn, String sql,
                                 ParamSetter setter, String cursorName)
            throws SQLException {
        PGobject cursor = new PGobject();
        cursor.setType("refcursor");
        cursor.setValue(cursorName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            ps.setObject(ps.getParameterMetaData().getParameterCount(), cursor);
            ps.execute();
        }
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
