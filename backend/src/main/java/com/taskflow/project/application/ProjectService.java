package com.taskflow.project.application;

import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.config.CacheConfig;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.project.api.CreateProjectRequest;
import com.taskflow.project.api.ProjectResponse;
import com.taskflow.project.api.UpdateProjectRequest;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.common.api.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final JdbcProjectRepository repository;
    private final JdbcMemberRepository memberRepository;

    public ProjectService(JdbcProjectRepository repository, JdbcMemberRepository memberRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
    }

    @CacheEvict(cacheNames = CacheConfig.PROJECTS_BY_USER, allEntries = true)
    public ProjectResponse createProject(CreateProjectRequest req, long callerId) {
        // The create_project_with_owner procedure (V6) inserts into both
        // projects and project_members atomically; no separate add() call needed.
        Project saved = repository.save(callerId, req.name().strip(), req.description());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PROJECTS_BY_USER,
               key = "#callerId + ':' + #page + ':' + #size")
    public PageResponse<ProjectResponse> listProjects(int page, int size, long callerId) {
        long total = repository.countByMemberId(callerId);
        List<ProjectResponse> content = repository
                .findByMemberId(callerId, size, page * size)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page, size, total);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(long id, long callerId) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
        requireMember(id, callerId);
        return toResponse(project);
    }

    @CacheEvict(cacheNames = CacheConfig.PROJECTS_BY_USER, allEntries = true)
    public ProjectResponse updateProject(long id, UpdateProjectRequest req, long callerId) {
        Project existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
        requireOwner(id, callerId);

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

    @CacheEvict(cacheNames = CacheConfig.PROJECTS_BY_USER, allEntries = true)
    public void deleteProject(long id, long callerId) {
        if (!repository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Project " + id + " not found");
        }
        requireOwner(id, callerId);
        repository.deleteById(id);
    }

    private void requireMember(long projectId, long callerId) {
        memberRepository.findByProjectAndUser(projectId, callerId)
                .orElseThrow(ForbiddenException::new);
    }

    private void requireOwner(long projectId, long callerId) {
        ProjectMember m = memberRepository.findByProjectAndUser(projectId, callerId)
                .orElseThrow(ForbiddenException::new);
        if (m.role() != MemberRole.OWNER) {
            throw new ForbiddenException();
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
