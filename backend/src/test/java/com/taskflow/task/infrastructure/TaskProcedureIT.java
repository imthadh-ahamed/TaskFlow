package com.taskflow.task.infrastructure;

import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Integration tests for task stored procedures (V10 migration):
 * SaveTask, GetTaskById, GetTasksByProjectId, CountTasksByProjectId,
 * UpdateTask, UpdateTaskStatus.
 *
 * <p>Requires a running PostgreSQL instance; skips gracefully otherwise.
 * Each test runs in a transaction that rolls back on completion.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class TaskProcedureIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcTaskRepository taskRepository;

    @Autowired
    JdbcProjectRepository projectRepository;

    private long ownerUserId;
    private long projectId;

    @BeforeAll
    static void checkDatabase(@Autowired DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            // connection succeeded — DB is reachable
        } catch (Exception e) {
            assumeTrue(false, "PostgreSQL unavailable — skipping integration tests: " + e.getMessage());
        }
    }

    @BeforeEach
    void setup() {
        ownerUserId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('task-it-owner@example.com', 'Task IT Owner', '$2a$10$test') " +
                "RETURNING id",
                Long.class);
        projectId = projectRepository.save(ownerUserId, "Task IT Project", null).id();
    }

    // --- SaveTask ---

    @Test
    void saveTask_returnsTaskWithCorrectFields() {
        Task task = taskRepository.save(projectId, "Fix bug", "body", TaskPriority.HIGH, null, null);

        assertThat(task.id()).isPositive();
        assertThat(task.projectId()).isEqualTo(projectId);
        assertThat(task.title()).isEqualTo("Fix bug");
        assertThat(task.description()).isEqualTo("body");
        assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        assertThat(task.createdAt()).isNotNull();
    }

    @Test
    void saveTask_nullDescriptionAndAssignee_isAccepted() {
        Task task = taskRepository.save(projectId, "Minimal task", null, TaskPriority.LOW, null, null);

        assertThat(task.description()).isNull();
        assertThat(task.assigneeId()).isNull();
    }

    // --- GetTaskById ---

    @Test
    void getTaskById_returnsTask() {
        long taskId = taskRepository.save(projectId, "Find me", null, TaskPriority.MEDIUM, null, null).id();

        Optional<Task> result = taskRepository.findById(taskId);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Find me");
        assertThat(result.get().projectId()).isEqualTo(projectId);
    }

    @Test
    void getTaskById_returnsEmpty_whenNotFound() {
        Optional<Task> result = taskRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // --- GetTasksByProjectId ---

    @Test
    void getTasksByProjectId_returnsSavedTasks() {
        taskRepository.save(projectId, "Task A", null, TaskPriority.MEDIUM, null, null);
        taskRepository.save(projectId, "Task B", null, TaskPriority.LOW, null, null);

        List<Task> tasks = taskRepository.findByProjectId(projectId, null, null, null, 10, 0);

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::projectId).containsOnly(projectId);
    }

    @Test
    void getTasksByProjectId_filtersOnStatus() {
        taskRepository.save(projectId, "Todo task", null, TaskPriority.MEDIUM, null, null);

        List<Task> todoTasks = taskRepository.findByProjectId(projectId, TaskStatus.TODO, null, null, 10, 0);
        List<Task> doneTasks = taskRepository.findByProjectId(projectId, TaskStatus.DONE, null, null, 10, 0);

        assertThat(todoTasks).hasSize(1);
        assertThat(doneTasks).isEmpty();
    }

    // --- CountTasksByProjectId ---

    @Test
    void countTasksByProjectId_returnsCorrectCount() {
        taskRepository.save(projectId, "Task 1", null, TaskPriority.MEDIUM, null, null);
        taskRepository.save(projectId, "Task 2", null, TaskPriority.MEDIUM, null, null);

        long count = taskRepository.countByProjectId(projectId, null, null, null);

        assertThat(count).isEqualTo(2L);
    }

    // --- UpdateTask ---

    @Test
    void updateTask_changesTitle() {
        long taskId = taskRepository.save(projectId, "Old title", null, TaskPriority.LOW, null, null).id();

        Optional<Task> updated = taskRepository.update(
                taskId, "New title", null, TaskPriority.HIGH, null, false, null, false);

        assertThat(updated).isPresent();
        assertThat(updated.get().title()).isEqualTo("New title");
        assertThat(updated.get().priority()).isEqualTo(TaskPriority.HIGH);
    }

    // --- UpdateTaskStatus ---

    @Test
    void updateTaskStatus_changesStatus() {
        long taskId = taskRepository.save(projectId, "Status task", null, TaskPriority.MEDIUM, null, null).id();

        Optional<Task> updated = taskRepository.updateStatus(taskId, TaskStatus.IN_PROGRESS);

        assertThat(updated).isPresent();
        assertThat(updated.get().status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
