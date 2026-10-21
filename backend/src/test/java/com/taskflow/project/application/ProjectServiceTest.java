package com.taskflow.project.application;

import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.project.api.CreateProjectRequest;
import com.taskflow.project.api.ProjectResponse;
import com.taskflow.project.api.UpdateProjectRequest;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    JdbcProjectRepository repository;

    @InjectMocks
    ProjectService service;

    private Project sampleProject(long id) {
        return new Project(id, 1L, "Alpha", "desc", Instant.EPOCH, Instant.EPOCH);
    }

    // --- createProject ---

    @Test
    void createProject_saves_and_returns_response() {
        when(repository.save(1L, "Alpha", "desc")).thenReturn(sampleProject(1L));

        ProjectResponse response = service.createProject(
                new CreateProjectRequest("Alpha", "desc", 1L));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alpha");
        verify(repository).save(1L, "Alpha", "desc");
    }

    @Test
    void createProject_strips_title_whitespace() {
        when(repository.save(1L, "Alpha", null)).thenReturn(sampleProject(1L));

        service.createProject(new CreateProjectRequest("  Alpha  ", null, 1L));

        verify(repository).save(1L, "Alpha", null);
    }

    // --- getProject ---

    @Test
    void getProject_returns_response_when_found() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleProject(1L)));

        ProjectResponse response = service.getProject(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getProject_throws_when_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getProject(99L))
                .withMessageContaining("99");
    }

    // --- listProjects ---

    @Test
    void listProjects_returns_paged_results() {
        when(repository.count()).thenReturn(3L);
        when(repository.findAll(2, 0)).thenReturn(
                List.of(sampleProject(1L), sampleProject(2L)));

        PageResponse<ProjectResponse> page = service.listProjects(0, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.total()).isEqualTo(3L);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    // --- updateProject ---

    @Test
    void updateProject_applies_new_name() {
        Project existing = sampleProject(1L);
        Project updated = new Project(1L, 1L, "Beta", "desc", Instant.EPOCH, Instant.EPOCH);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.update(1L, "Beta", "desc")).thenReturn(Optional.of(updated));

        ProjectResponse response = service.updateProject(1L,
                new UpdateProjectRequest("Beta", null));

        assertThat(response.name()).isEqualTo("Beta");
    }

    @Test
    void updateProject_clears_description_when_empty_string_sent() {
        Project existing = sampleProject(1L);
        Project updated = new Project(1L, 1L, "Alpha", null, Instant.EPOCH, Instant.EPOCH);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.update(1L, "Alpha", null)).thenReturn(Optional.of(updated));

        service.updateProject(1L, new UpdateProjectRequest(null, ""));

        verify(repository).update(1L, "Alpha", null);
    }

    @Test
    void updateProject_throws_when_project_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.updateProject(99L,
                        new UpdateProjectRequest("New", null)));
    }

    @Test
    void updateProject_throws_when_provided_name_is_blank() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleProject(1L)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.updateProject(1L,
                        new UpdateProjectRequest("   ", null)));
    }

    // --- deleteProject ---

    @Test
    void deleteProject_succeeds_when_project_exists() {
        when(repository.deleteById(1L)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.deleteProject(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void deleteProject_throws_when_not_found() {
        when(repository.deleteById(99L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.deleteProject(99L));
    }
}
