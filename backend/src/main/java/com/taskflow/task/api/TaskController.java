package com.taskflow.task.api;

import com.taskflow.common.api.ErrorResponse;
import com.taskflow.common.api.PageResponse;
import com.taskflow.task.application.TaskService;
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

@Tag(name = "Tasks", description = "Create, read, update tasks and move them across statuses")
@RestController
@Validated
class TaskController {

    private final TaskService service;

    TaskController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "Create a task in a project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/projects/{projectId}/tasks")
    ResponseEntity<TaskResponse> create(
            @PathVariable long projectId,
            @Valid @RequestBody CreateTaskRequest req) {
        TaskResponse created = service.createTask(projectId, req);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/tasks/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List tasks in a project",
               description = "Returns a paginated list of tasks. All filter params are optional and combinable.")
    @ApiResponse(responseCode = "200", description = "Page of tasks")
    @GetMapping("/api/v1/projects/{projectId}/tasks")
    ResponseEntity<PageResponse<TaskResponse>> list(
            @PathVariable long projectId,
            @Parameter(description = "Filter by status (todo, in_progress, in_review, done)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority (low, medium, high, urgent)")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by assignee user ID")
            @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(
                service.listTasks(projectId, status, priority, assigneeId, page, size));
    }

    @Operation(summary = "Get a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<TaskResponse> getOne(@PathVariable long taskId) {
        return ResponseEntity.ok(service.getTask(taskId));
    }

    @Operation(summary = "Update a task",
               description = "Partial update — omitted fields are left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<TaskResponse> update(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskRequest req) {
        return ResponseEntity.ok(service.updateTask(taskId, req));
    }

    @Operation(summary = "Move a task to a new status",
               description = "Dedicated endpoint for board-style drag-and-drop status transitions. "
                           + "`status` must be one of TODO, IN_PROGRESS, IN_REVIEW, DONE (uppercase).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/tasks/{taskId}/status")
    ResponseEntity<TaskResponse> updateStatus(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(taskId, req));
    }
}
