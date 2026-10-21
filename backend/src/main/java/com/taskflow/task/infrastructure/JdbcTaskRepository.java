package com.taskflow.task.infrastructure;

import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
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
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.SaveTask(?, ?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                cs.setString(2, title);
                if (description != null) cs.setString(3, description);
                else cs.setNull(3, Types.VARCHAR);
                cs.setString(4, priority.name().toLowerCase());
                if (assigneeId != null) cs.setLong(5, assigneeId);
                else cs.setNull(5, Types.BIGINT);
                if (dueDate != null) cs.setDate(6, Date.valueOf(dueDate));
                else cs.setNull(6, Types.DATE);
                cs.registerOutParameter(7, Types.OTHER);
                cs.execute();
                return fetchList(conn, cs.getString(7), rowMapper()).get(0);
            }
        });
    }

    public Optional<Task> findById(long id) {
        return jdbc.execute((ConnectionCallback<Optional<Task>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetTaskById(?, ?)")) {
                cs.setLong(1, id);
                cs.registerOutParameter(2, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(2), rowMapper());
            }
        });
    }

    public List<Task> findByProjectId(long projectId,
                                      TaskStatus statusFilter,
                                      TaskPriority priorityFilter,
                                      Long assigneeIdFilter,
                                      int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<Task>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetTasksByProjectId(?, ?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                setNullableString(cs, 2, statusFilter != null ? statusFilter.name().toLowerCase() : null);
                setNullableString(cs, 3, priorityFilter != null ? priorityFilter.name().toLowerCase() : null);
                setNullableLong(cs, 4, assigneeIdFilter);
                cs.setInt(5, limit);
                cs.setInt(6, offset);
                cs.registerOutParameter(7, Types.OTHER);
                cs.execute();
                return fetchList(conn, cs.getString(7), rowMapper());
            }
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
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.UpdateTask(?, ?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, id);
                cs.setString(2, title);
                if (description != null) cs.setString(3, description);
                else cs.setNull(3, Types.VARCHAR);
                cs.setString(4, priority.name().toLowerCase());
                setNullableLong(cs, 5, resolvedAssigneeId);
                if (resolvedDueDate != null) cs.setDate(6, resolvedDueDate);
                else cs.setNull(6, Types.DATE);
                cs.registerOutParameter(7, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(7), rowMapper());
            }
        });
    }

    public Optional<Task> updateStatus(long id, TaskStatus status) {
        return jdbc.execute((ConnectionCallback<Optional<Task>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.UpdateTaskStatus(?, ?, ?)")) {
                cs.setLong(1, id);
                cs.setString(2, status.name().toLowerCase());
                cs.registerOutParameter(3, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(3), rowMapper());
            }
        });
    }

    public List<TaskWithAssignee> findByProjectIdWithAssignee(long projectId,
                                                               TaskStatus statusFilter,
                                                               TaskPriority priorityFilter,
                                                               Long assigneeIdFilter,
                                                               int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<TaskWithAssignee>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetTasksByProjectIdWithAssignee(?, ?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                setNullableString(cs, 2, statusFilter != null ? statusFilter.name().toLowerCase() : null);
                setNullableString(cs, 3, priorityFilter != null ? priorityFilter.name().toLowerCase() : null);
                setNullableLong(cs, 4, assigneeIdFilter);
                cs.setInt(5, limit);
                cs.setInt(6, offset);
                cs.registerOutParameter(7, Types.OTHER);
                cs.execute();
                return fetchList(conn, cs.getString(7), enrichedRowMapper());
            }
        });
    }

    public Optional<TaskWithAssignee> findByIdWithAssignee(long id) {
        return jdbc.execute((ConnectionCallback<Optional<TaskWithAssignee>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetTaskByIdWithAssignee(?, ?)")) {
                cs.setLong(1, id);
                cs.registerOutParameter(2, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(2), enrichedRowMapper());
            }
        });
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private static void setNullableString(CallableStatement cs, int idx, String value)
            throws java.sql.SQLException {
        if (value != null) cs.setString(idx, value);
        else cs.setNull(idx, Types.VARCHAR);
    }

    private static void setNullableLong(CallableStatement cs, int idx, Long value)
            throws java.sql.SQLException {
        if (value != null) cs.setLong(idx, value);
        else cs.setNull(idx, Types.BIGINT);
    }

    private <T> List<T> fetchList(java.sql.Connection conn, String cursorName,
                                   RowMapper<T> mapper) throws java.sql.SQLException {
        try (Statement fetch = conn.createStatement();
             ResultSet rs = fetch.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.mapRow(rs, results.size()));
            }
            return results;
        }
    }

    private <T> Optional<T> fetchOptional(java.sql.Connection conn, String cursorName,
                                           RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> rows = fetchList(conn, cursorName, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
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
