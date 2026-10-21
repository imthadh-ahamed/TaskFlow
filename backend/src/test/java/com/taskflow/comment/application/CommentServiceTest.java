package com.taskflow.comment.application;

import com.taskflow.comment.api.CommentResponse;
import com.taskflow.comment.api.CreateCommentRequest;
import com.taskflow.comment.domain.Comment;
import com.taskflow.comment.infrastructure.JdbcCommentRepository;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.JdbcTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock JdbcCommentRepository commentRepository;
    @Mock JdbcTaskRepository taskRepository;
    @InjectMocks CommentService service;

    private Task sampleTask() {
        return new Task(1L, 1L, null, "Fix bug", null,
                TaskStatus.TODO, TaskPriority.MEDIUM, null, Instant.EPOCH, Instant.EPOCH);
    }

    private Comment sampleComment(long id) {
        return new Comment(id, 1L, 42L, "Looks good", Instant.EPOCH, Instant.EPOCH);
    }

    // --- createComment ---

    @Test
    void createComment_saves_when_task_exists() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask()));
        when(commentRepository.save(eq(1L), eq(42L), anyString()))
                .thenReturn(sampleComment(10L));

        CommentResponse resp = service.createComment(1L,
                new CreateCommentRequest("Looks good", 42L));

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.body()).isEqualTo("Looks good");
        assertThat(resp.authorId()).isEqualTo(42L);
    }

    @Test
    void createComment_throws_when_task_not_found() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.createComment(99L,
                        new CreateCommentRequest("body", 1L)))
                .withMessageContaining("99");
    }

    @Test
    void createComment_strips_whitespace_from_body() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask()));
        when(commentRepository.save(eq(1L), eq(42L), eq("Looks good")))
                .thenReturn(sampleComment(10L));

        service.createComment(1L, new CreateCommentRequest("  Looks good  ", 42L));

        verify(commentRepository).save(1L, 42L, "Looks good");
    }

    // --- listComments ---

    @Test
    void listComments_returns_paged_results() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask()));
        when(commentRepository.countByTaskId(1L)).thenReturn(2L);
        when(commentRepository.findByTaskId(1L, 20, 0))
                .thenReturn(List.of(sampleComment(1L), sampleComment(2L)));

        PageResponse<CommentResponse> page = service.listComments(1L, 0, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.total()).isEqualTo(2L);
    }

    @Test
    void listComments_throws_when_task_not_found() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.listComments(99L, 0, 20));
    }
}
