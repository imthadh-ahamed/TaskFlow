package com.taskflow.task.api;

import com.taskflow.common.api.PageResponse;
import com.taskflow.task.application.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Validated
class TaskController {

    private final TaskService service;

    TaskController(TaskService service) {
        this.service = service;
    }

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

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    ResponseEntity<PageResponse<TaskResponse>> list(
            @PathVariable long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(
                service.listTasks(projectId, status, priority, assigneeId, page, size));
    }

    @GetMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<TaskResponse> getOne(@PathVariable long taskId) {
        return ResponseEntity.ok(service.getTask(taskId));
    }

    @PatchMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<TaskResponse> update(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskRequest req) {
        return ResponseEntity.ok(service.updateTask(taskId, req));
    }

    @PatchMapping("/api/v1/tasks/{taskId}/status")
    ResponseEntity<TaskResponse> updateStatus(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(taskId, req));
    }
}
