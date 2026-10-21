package com.taskflow.comment.api;

import com.taskflow.comment.application.CommentService;
import com.taskflow.common.api.PageResponse;
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
class CommentController {

    private final CommentService service;

    CommentController(CommentService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/tasks/{taskId}/comments")
    ResponseEntity<CommentResponse> create(
            @PathVariable long taskId,
            @Valid @RequestBody CreateCommentRequest req) {
        CommentResponse created = service.createComment(taskId, req);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/tasks/{taskId}/comments/{id}")
                .buildAndExpand(taskId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/api/v1/tasks/{taskId}/comments")
    ResponseEntity<PageResponse<CommentResponse>> list(
            @PathVariable long taskId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listComments(taskId, page, size));
    }
}
