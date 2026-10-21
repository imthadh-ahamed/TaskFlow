package com.taskflow.comment.infrastructure;

import com.taskflow.comment.domain.Comment;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.infrastructure.JdbcTaskRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for comment stored procedures (V7 GetCommentsTaskId,
 * V11 SaveComment / CountCommentsByTaskId).
 *
 * <p>Requires a running PostgreSQL instance; skips gracefully otherwise.
 * Each test runs in a transaction that rolls back on completion.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class CommentProcedureIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcCommentRepository commentRepository;

    @Autowired
    JdbcTaskRepository taskRepository;

    @Autowired
    JdbcProjectRepository projectRepository;

    private long authorUserId;
    private long taskId;

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
        authorUserId = jdbc.queryForObject(
                "INSERT INTO public.users (email, display_name, password_hash) " +
                "VALUES ('comment-it-author@example.com', 'Comment Author', '$2a$10$test') " +
                "RETURNING id",
                Long.class);

        long projectId = projectRepository.save(authorUserId, "Comment IT Project", null).id();
        taskId = taskRepository.save(projectId, "Comment IT Task", null, TaskPriority.MEDIUM, null, null).id();
    }

    // --- SaveComment (V11) ---

    @Test
    void saveComment_returnsCommentWithCorrectFields() {
        Comment comment = commentRepository.save(taskId, authorUserId, "Great work!");

        assertThat(comment.id()).isPositive();
        assertThat(comment.taskId()).isEqualTo(taskId);
        assertThat(comment.authorId()).isEqualTo(authorUserId);
        assertThat(comment.body()).isEqualTo("Great work!");
        assertThat(comment.createdAt()).isNotNull();
        assertThat(comment.updatedAt()).isNotNull();
    }

    @Test
    void saveComment_allowsMultipleCommentsOnSameTask() {
        commentRepository.save(taskId, authorUserId, "First comment");
        commentRepository.save(taskId, authorUserId, "Second comment");

        long count = commentRepository.countByTaskId(taskId);
        assertThat(count).isEqualTo(2L);
    }

    // --- GetCommentsTaskId (V7) ---

    @Test
    void getCommentsTaskId_returnsSavedComments() {
        commentRepository.save(taskId, authorUserId, "Alpha");
        commentRepository.save(taskId, authorUserId, "Beta");

        List<Comment> comments = commentRepository.findByTaskId(taskId, 10, 0);

        assertThat(comments).hasSize(2);
        assertThat(comments).extracting(Comment::taskId).containsOnly(taskId);
    }

    @Test
    void getCommentsTaskId_respectsPagination() {
        commentRepository.save(taskId, authorUserId, "Comment 1");
        commentRepository.save(taskId, authorUserId, "Comment 2");
        commentRepository.save(taskId, authorUserId, "Comment 3");

        List<Comment> firstPage = commentRepository.findByTaskId(taskId, 2, 0);
        List<Comment> secondPage = commentRepository.findByTaskId(taskId, 2, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
    }

    @Test
    void getCommentsTaskId_returnsEmpty_whenNoComments() {
        List<Comment> comments = commentRepository.findByTaskId(taskId, 10, 0);

        assertThat(comments).isEmpty();
    }

    // --- CountCommentsByTaskId (V11) ---

    @Test
    void countCommentsByTaskId_returnsZero_whenNoComments() {
        long count = commentRepository.countByTaskId(taskId);

        assertThat(count).isZero();
    }

    @Test
    void countCommentsByTaskId_returnsAccurateCount() {
        commentRepository.save(taskId, authorUserId, "C1");
        commentRepository.save(taskId, authorUserId, "C2");
        commentRepository.save(taskId, authorUserId, "C3");

        long count = commentRepository.countByTaskId(taskId);

        assertThat(count).isEqualTo(3L);
    }
}
