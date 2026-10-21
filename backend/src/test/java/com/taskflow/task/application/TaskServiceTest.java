package com.taskflow.task.application;

import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.task.api.*;
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
class TaskServiceTest {

    @Mock JdbcTaskRepository taskRepository;
    @Mock JdbcProjectRepository projectRepository;
    @Mock JdbcMemberRepository memberRepository;
    @InjectMocks TaskService service;

    private Project sampleProject() {
        return new Project(1L, 1L, "P", null, Instant.EPOCH, Instant.EPOCH);
    }

    private Task sampleTask(long id, TaskStatus status) {
        return new Task(id, 1L, null, "Fix bug", null,
                status, TaskPriority.MEDIUM, null, Instant.EPOCH, Instant.EPOCH);
    }

    private ProjectMember ownerMember() {
        return new ProjectMember(1L, 1L, MemberRole.OWNER, Instant.EPOCH);
    }

    // --- createTask ---

    @Test
    void createTask_saves_when_project_exists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));
        when(taskRepository.save(anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(sampleTask(10L, TaskStatus.TODO));

        TaskResponse resp = service.createTask(1L,
                new CreateTaskRequest("Fix bug", null, null, null, null), 1L);

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.status()).isEqualTo("todo");
    }

    @Test
    void createTask_throws_when_project_not_found() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.createTask(99L,
                        new CreateTaskRequest("Fix bug", null, null, null, null), 1L))
                .withMessageContaining("99");
    }

    @Test
    void createTask_defaults_priority_to_medium_when_null() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));
        when(taskRepository.save(eq(1L), anyString(), any(), eq(TaskPriority.MEDIUM), any(), any()))
                .thenReturn(sampleTask(10L, TaskStatus.TODO));

        service.createTask(1L, new CreateTaskRequest("Fix bug", null, null, null, null), 1L);

        verify(taskRepository).save(eq(1L), anyString(), any(), eq(TaskPriority.MEDIUM), any(), any());
    }

    // --- listTasks ---

    @Test
    void listTasks_returns_paged_results() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));
        when(taskRepository.countByProjectId(1L, null, null, null)).thenReturn(1L);
        when(taskRepository.findByProjectId(eq(1L), isNull(), isNull(), isNull(), eq(20), eq(0)))
                .thenReturn(List.of(sampleTask(1L, TaskStatus.TODO)));

        PageResponse<TaskResponse> page = service.listTasks(1L, null, null, null, 0, 20, 1L);

        assertThat(page.content()).hasSize(1);
        assertThat(page.total()).isEqualTo(1L);
    }

    @Test
    void listTasks_throws_for_invalid_status_filter() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.listTasks(1L, "invalid", null, null, 0, 20, 1L))
                .withMessageContaining("Invalid status filter");
    }

    // --- getTask ---

    @Test
    void getTask_returns_response_when_found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));

        TaskResponse resp = service.getTask(1L, 1L);

        assertThat(resp.id()).isEqualTo(1L);
    }

    @Test
    void getTask_throws_when_not_found() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getTask(99L, 1L));
    }

    // --- updateTask ---

    @Test
    void updateTask_applies_new_title() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));
        when(taskRepository.update(anyLong(), eq("New title"), any(), any(), any(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));

        TaskResponse resp = service.updateTask(1L,
                new UpdateTaskRequest("New title", null, null, null, false, null), 1L);

        assertThat(resp).isNotNull();
    }

    @Test
    void updateTask_throws_when_provided_title_is_blank() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.updateTask(1L,
                        new UpdateTaskRequest("   ", null, null, null, false, null), 1L));
    }

    // --- updateStatus ---

    @Test
    void updateStatus_applies_valid_transition() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));
        when(taskRepository.updateStatus(1L, TaskStatus.IN_PROGRESS))
                .thenReturn(Optional.of(sampleTask(1L, TaskStatus.IN_PROGRESS)));

        TaskResponse resp = service.updateStatus(1L,
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS), 1L);

        assertThat(resp.status()).isEqualTo("in_progress");
    }

    @Test
    void updateStatus_rejects_invalid_transition() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask(1L, TaskStatus.TODO)));
        when(memberRepository.findByProjectAndUser(1L, 1L)).thenReturn(Optional.of(ownerMember()));

        assertThatIllegalStateException()
                .isThrownBy(() -> service.updateStatus(1L,
                        new UpdateTaskStatusRequest(TaskStatus.DONE), 1L))
                .withMessageContaining("TODO")
                .withMessageContaining("DONE");
    }
}
