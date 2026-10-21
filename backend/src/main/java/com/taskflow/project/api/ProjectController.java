package com.taskflow.project.api;

import com.taskflow.common.api.PageResponse;
import com.taskflow.project.application.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
class ProjectController {

    private final ProjectService service;

    ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest req) {
        ProjectResponse created = service.createProject(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    ResponseEntity<PageResponse<ProjectResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listProjects(page, size));
    }

    @GetMapping("/{id}")
    ResponseEntity<ProjectResponse> getOne(@PathVariable long id) {
        return ResponseEntity.ok(service.getProject(id));
    }

    @PatchMapping("/{id}")
    ResponseEntity<ProjectResponse> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(service.updateProject(id, req));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
