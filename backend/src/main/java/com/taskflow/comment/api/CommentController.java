package com.taskflow.comment.api;

import com.taskflow.comment.application.CommentService;
import com.taskflow.common.api.ErrorResponse;
import com.taskflow.common.api.PageResponse;
import com.taskflow.common.security.AuthenticatedUser;
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

@Tag(name = "Comments", description = "Post and list comments on tasks")
@RestController
@Validated
class CommentController {

        private final CommentService service;

        CommentController(CommentService service) {
                this.service = service;
        }

        // /api/v1/tasks/{taskId}/comments - Create a comment on a task
        @Operation(summary = "Post a comment on a task")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Comment created"),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Not a writable member of this task's project", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping("/api/v1/tasks/{taskId}/comments")
        ResponseEntity<CommentResponse> create(
                        @PathVariable long taskId,
                        @Valid @RequestBody CreateCommentRequest req) {
                CommentResponse created = service.createComment(taskId, req, AuthenticatedUser.currentUserId());
                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/v1/tasks/{taskId}/comments/{id}")
                                .buildAndExpand(taskId, created.id())
                                .toUri();
                return ResponseEntity.created(location).body(created);
        }

        // /api/v1/tasks/{taskId}/comments - List comments on a task
        @Operation(summary = "List comments on a task", description = "Returns a paginated list of comments ordered by creation date ascending.")
        @ApiResponse(responseCode = "200", description = "Page of comments")
        @GetMapping("/api/v1/tasks/{taskId}/comments")
        ResponseEntity<PageResponse<CommentResponse>> list(
                        @PathVariable long taskId,
                        @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
                        @Parameter(description = "Page size (1–100)", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
                return ResponseEntity.ok(service.listComments(taskId, page, size, AuthenticatedUser.currentUserId()));
        }
}
