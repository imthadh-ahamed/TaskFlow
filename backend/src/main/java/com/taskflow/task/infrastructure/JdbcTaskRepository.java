package com.taskflow.task.infrastructure;

import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
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
        String sql = """
                INSERT INTO tasks (project_id, title, description, status, priority, assignee_id, due_date)
                VALUES (?, ?, ?, 'todo', ?, ?, ?)
                RETURNING id, project_id, assignee_id, title, description, status, priority,
                          due_date, created_at, updated_at
                """;
        return jdbc.queryForObject(sql, rowMapper(),
                projectId, title, description,
                priority.name().toLowerCase(),
                assigneeId,
                dueDate != null ? Date.valueOf(dueDate) : null);
    }

    public Optional<Task> findById(long id) {
        String sql = """
                SELECT id, project_id, assignee_id, title, description, status, priority,
                       due_date, created_at, updated_at
                FROM tasks WHERE id = ?
                """;
        return jdbc.query(sql, rowMapper(), id).stream().findFirst();
    }

    public List<Task> findByProjectId(long projectId,
                                      TaskStatus statusFilter,
                                      TaskPriority priorityFilter,
                                      Long assigneeIdFilter,
                                      int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, project_id, assignee_id, title, description, status, priority,
                       due_date, created_at, updated_at
                FROM tasks WHERE project_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        appendFilters(sql, params, statusFilter, priorityFilter, assigneeIdFilter);
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbc.query(sql.toString(), rowMapper(), params.toArray());
    }

    public long countByProjectId(long projectId,
                                 TaskStatus statusFilter,
                                 TaskPriority priorityFilter,
                                 Long assigneeIdFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM tasks WHERE project_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        appendFilters(sql, params, statusFilter, priorityFilter, assigneeIdFilter);
        Long n = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return n != null ? n : 0L;
    }

    public Optional<Task> update(long id, String title, String description,
                                 TaskPriority priority, Long assigneeId,
                                 boolean clearAssignee, LocalDate dueDate,
                                 boolean clearDueDate) {
        String sql = """
                UPDATE tasks
                SET title       = ?,
                    description = ?,
                    priority    = ?,
                    assignee_id = ?,
                    due_date    = ?,
                    updated_at  = now()
                WHERE id = ?
                RETURNING id, project_id, assignee_id, title, description, status, priority,
                          due_date, created_at, updated_at
                """;
        Long newAssigneeId = clearAssignee ? null : assigneeId;
        Date newDueDate = clearDueDate ? null : (dueDate != null ? Date.valueOf(dueDate) : null);
        return jdbc.query(sql, rowMapper(),
                title, description, priority.name().toLowerCase(),
                newAssigneeId, newDueDate, id)
                .stream().findFirst();
    }

    public Optional<Task> updateStatus(long id, TaskStatus status) {
        String sql = """
                UPDATE tasks
                SET status = ?, updated_at = now()
                WHERE id = ?
                RETURNING id, project_id, assignee_id, title, description, status, priority,
                          due_date, created_at, updated_at
                """;
        return jdbc.query(sql, rowMapper(), status.name().toLowerCase(), id)
                .stream().findFirst();
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                                TaskStatus statusFilter, TaskPriority priorityFilter,
                                Long assigneeIdFilter) {
        if (statusFilter != null) {
            sql.append(" AND status = ?");
            params.add(statusFilter.name().toLowerCase());
        }
        if (priorityFilter != null) {
            sql.append(" AND priority = ?");
            params.add(priorityFilter.name().toLowerCase());
        }
        if (assigneeIdFilter != null) {
            sql.append(" AND assignee_id = ?");
            params.add(assigneeIdFilter);
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
}
