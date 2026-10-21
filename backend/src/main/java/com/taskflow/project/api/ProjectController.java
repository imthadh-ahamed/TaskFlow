package com.taskflow.project.api;

import com.taskflow.common.api.ErrorResponse;
import com.taskflow.common.api.PageResponse;
import com.taskflow.project.application.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Projects", description = "Create, read, update and delete projects")
@RestController
@RequestMapping("/api/v1/projects")
@Validated
class ProjectController {

    private final ProjectService service;

    ProjectController(ProjectService service) {
        this.service = service;
    }

    @Operation(summary = "Create a project",
               description = "Creates a new project. `ownerId` is a dev-stage field that will be "
                           + "replaced by the authenticated user's ID once auth is added.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest req) {
        ProjectResponse created = service.createProject(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List projects",
               description = "Returns a paginated list of all projects ordered by creation date descending.")
    @ApiResponse(responseCode = "200", description = "Page of projects")
    @GetMapping
    ResponseEntity<PageResponse<ProjectResponse>> list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listProjects(page, size));
    }

    @Operation(summary = "Get a project by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project found"),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<ProjectResponse> getOne(@PathVariable long id) {
        return ResponseEntity.ok(service.getProject(id));
    }

    @Operation(summary = "Update a project",
               description = "Partial update — omitted fields are left unchanged. "
                           + "Send an empty string to clear `description`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    ResponseEntity<ProjectResponse> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(service.updateProject(id, req));
    }

    @Operation(summary = "Delete a project",
               description = "Permanently deletes a project and all its tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
