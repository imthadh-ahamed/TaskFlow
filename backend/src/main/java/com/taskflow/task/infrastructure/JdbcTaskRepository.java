package com.taskflow.task.infrastructure;

import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTaskRepository {

    private final JdbcTemplate jdbc;

    public JdbcTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Task save(long projectId, String title, String description,
                     TaskPriority priority, Long assigneeId, LocalDate dueDate) {
        return jdbc.execute((ConnectionCallback<Task>) conn -> {
            callWithCursor(conn, "CALL public.SaveTask(?, ?, ?, ?, ?, ?, ?)", ps -> {
                ps.setLong(1, projectId);
                ps.setString(2, title);
                if (description != null) ps.setString(3, description);
                else ps.setNull(3, Types.VARCHAR);
                ps.setString(4, priority.name().toLowerCase());
                if (assigneeId != null) ps.setLong(5, assigneeId);
                else ps.setNull(5, Types.BIGINT);
                if (dueDate != null) ps.setDate(6, Date.valueOf(dueDate));
                else ps.setNull(6, Types.DATE);
            }, "save_task_cursor");
            return fetchList(conn, "save_task_cursor", rowMapper()).get(0);
        });
    }

    public Optional<Task> findById(long id) {
        return jdbc.execute((ConnectionCallback<Optional<Task>>) conn -> {
            callWithCursor(conn, "CALL public.GetTaskById(?, ?)",
                    ps -> ps.setLong(1, id), "get_task_by_id_cursor");
            return fetchOptional(conn, "get_task_by_id_cursor", rowMapper());
        });
    }

    public List<Task> findByProjectId(long projectId,
                                      TaskStatus statusFilter,
                                      TaskPriority priorityFilter,
                                      Long assigneeIdFilter,
                                      int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<Task>>) conn -> {
            callWithCursor(conn, "CALL public.GetTasksByProjectId(?, ?, ?, ?, ?, ?, ?)", ps -> {
                ps.setLong(1, projectId);
                setNullableString(ps, 2, statusFilter != null ? statusFilter.name().toLowerCase() : null);
                setNullableString(ps, 3, priorityFilter != null ? priorityFilter.name().toLowerCase() : null);
                setNullableLong(ps, 4, assigneeIdFilter);
                ps.setInt(5, limit);
                ps.setInt(6, offset);
            }, "get_tasks_cursor");
            return fetchList(conn, "get_tasks_cursor", rowMapper());
        });
    }

    public long countByProjectId(long projectId,
                                 TaskStatus statusFilter,
                                 TaskPriority priorityFilter,
                                 Long assigneeIdFilter) {
        return jdbc.execute((ConnectionCallback<Long>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.CountTasksByProjectId(?, ?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                setNullableString(cs, 2, statusFilter != null ? statusFilter.name().toLowerCase() : null);
                setNullableString(cs, 3, priorityFilter != null ? priorityFilter.name().toLowerCase() : null);
                setNullableLong(cs, 4, assigneeIdFilter);
                cs.setNull(5, Types.BIGINT);
                cs.registerOutParameter(5, Types.BIGINT);
                cs.execute();
                return cs.getLong(5);
            }
        });
    }

    public Optional<Task> update(long id, String title, String description,
                                 TaskPriority priority, Long assigneeId,
                                 boolean clearAssignee, LocalDate dueDate,
                                 boolean clearDueDate) {
        Long resolvedAssigneeId = clearAssignee ? null : assigneeId;
        Date resolvedDueDate   = clearDueDate   ? null : (dueDate != null ? Date.valueOf(dueDate) : null);
        return jdbc.execute((ConnectionCallback<Optional<Task>>) conn -> {
            callWithCursor(conn, "CALL public.UpdateTask(?, ?, ?, ?, ?, ?, ?)", ps -> {
                ps.setLong(1, id);
                ps.setString(2, title);
                if (description != null) ps.setString(3, description);
                else ps.setNull(3, Types.VARCHAR);
                ps.setString(4, priority.name().toLowerCase());
                setNullableLong(ps, 5, resolvedAssigneeId);
                if (resolvedDueDate != null) ps.setDate(6, resolvedDueDate);
                else ps.setNull(6, Types.DATE);
            }, "update_task_cursor");
            return fetchOptional(conn, "update_task_cursor", rowMapper());
        });
    }

    public Optional<Task> updateStatus(long id, TaskStatus status) {
        return jdbc.execute((ConnectionCallback<Optional<Task>>) conn -> {
            callWithCursor(conn, "CALL public.UpdateTaskStatus(?, ?, ?)", ps -> {
                ps.setLong(1, id);
                ps.setString(2, status.name().toLowerCase());
            }, "update_task_status_cursor");
            return fetchOptional(conn, "update_task_status_cursor", rowMapper());
        });
    }

    public List<TaskWithAssignee> findByProjectIdWithAssignee(long projectId,
                                                               TaskStatus statusFilter,
                                                               TaskPriority priorityFilter,
                                                               Long assigneeIdFilter,
                                                               int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<TaskWithAssignee>>) conn -> {
            callWithCursor(conn, "CALL public.GetTasksByProjectIdWithAssignee(?, ?, ?, ?, ?, ?, ?)", ps -> {
                ps.setLong(1, projectId);
                setNullableString(ps, 2, statusFilter != null ? statusFilter.name().toLowerCase() : null);
                setNullableString(ps, 3, priorityFilter != null ? priorityFilter.name().toLowerCase() : null);
                setNullableLong(ps, 4, assigneeIdFilter);
                ps.setInt(5, limit);
                ps.setInt(6, offset);
            }, "get_tasks_with_assignee_cursor");
            return fetchList(conn, "get_tasks_with_assignee_cursor", enrichedRowMapper());
        });
    }

    public Optional<TaskWithAssignee> findByIdWithAssignee(long id) {
        return jdbc.execute((ConnectionCallback<Optional<TaskWithAssignee>>) conn -> {
            callWithCursor(conn, "CALL public.GetTaskByIdWithAssignee(?, ?)",
                    ps -> ps.setLong(1, id), "get_task_with_assignee_cursor");
            return fetchOptional(conn, "get_task_with_assignee_cursor", enrichedRowMapper());
        });
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private static void setNullableString(PreparedStatement ps, int idx, String value)
            throws java.sql.SQLException {
        if (value != null) ps.setString(idx, value);
        else ps.setNull(idx, Types.VARCHAR);
    }

    private static void setNullableLong(PreparedStatement ps, int idx, Long value)
            throws java.sql.SQLException {
        if (value != null) ps.setLong(idx, value);
        else ps.setNull(idx, Types.BIGINT);
    }

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

    private <T> Optional<T> fetchOptional(java.sql.Connection conn, String cursorName,
                                           RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> rows = fetchList(conn, cursorName, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
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

    private RowMapper<Task> rowMapper() {
        return (rs, rowNum) -> {
            Date sqlDueDate = rs.getDate("due_date");
            return new Task(
                    rs.getLong("id"),
                    rs.getLong("project_id"),
                    rs.getObject("assignee_id", Long.class),
                    rs.getString("title"),
                    rs.getString("description"),
                    TaskStatus.valueOf(rs.getString("status").toUpperCase()),
                    TaskPriority.valueOf(rs.getString("priority").toUpperCase()),
                    sqlDueDate != null ? sqlDueDate.toLocalDate() : null,
                    rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("updated_at", OffsetDateTime.class).toInstant()
            );
        };
    }

    private RowMapper<TaskWithAssignee> enrichedRowMapper() {
        return (rs, rowNum) -> new TaskWithAssignee(rowMapper().mapRow(rs, rowNum),
                rs.getString("assignee_name"));
    }
}
