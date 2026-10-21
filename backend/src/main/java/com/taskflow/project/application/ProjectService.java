package com.taskflow.project.application;

import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.project.api.CreateProjectRequest;
import com.taskflow.project.api.ProjectResponse;
import com.taskflow.project.api.UpdateProjectRequest;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.common.api.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final JdbcProjectRepository repository;

    public ProjectService(JdbcProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectResponse createProject(CreateProjectRequest req) {
        Project saved = repository.save(req.ownerId(), req.name().strip(), req.description());
        return toResponse(saved);
    }

    public PageResponse<ProjectResponse> listProjects(int page, int size) {
        long total = repository.count();
        List<ProjectResponse> content = repository
                .findAll(size, page * size)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page, size, total);
    }

    public ProjectResponse getProject(long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }

    public ProjectResponse updateProject(long id, UpdateProjectRequest req) {
        Project existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));

        String newName = (req.name() != null) ? req.name().strip() : existing.name();
        if (newName.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        if (newName.length() > 200) {
            throw new IllegalArgumentException("Project name must not exceed 200 characters");
        }

        // null = leave unchanged; empty string = clear to null
        String newDescription;
        if (req.description() == null) {
            newDescription = existing.description();
        } else {
            newDescription = req.description().isBlank() ? null : req.description().strip();
        }

        Project updated = repository.update(id, newName, newDescription)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
        return toResponse(updated);
    }

    public void deleteProject(long id) {
        if (!repository.deleteById(id)) {
            throw new ResourceNotFoundException("Project " + id + " not found");
        }
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(
                p.id(),
                p.ownerId(),
                p.name(),
                p.description(),
                p.createdAt().toString(),
                p.updatedAt().toString()
        );
    }
}
