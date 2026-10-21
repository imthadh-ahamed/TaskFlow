package com.taskflow.membership.api;

import com.taskflow.common.api.ErrorResponse;
import com.taskflow.membership.application.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Members", description = "Manage project membership and roles")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
class MemberController {

    private final MemberService service;

    MemberController(MemberService service) {
        this.service = service;
    }

    @Operation(summary = "Add a member to a project",
               description = "Adds a user to the project with the given role. "
                           + "Role must be MEMBER or VIEWER — OWNER is assigned only on project creation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    ResponseEntity<MemberResponse> add(
            @PathVariable long projectId,
            @Valid @RequestBody AddMemberRequest req) {
        MemberResponse created = service.addMember(projectId, req);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/projects/{projectId}/members/{userId}")
                .buildAndExpand(projectId, created.userId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List all members of a project")
    @ApiResponse(responseCode = "200", description = "Member list")
    @GetMapping
    ResponseEntity<List<MemberResponse>> list(@PathVariable long projectId) {
        return ResponseEntity.ok(service.listMembers(projectId));
    }

    @Operation(summary = "Update a member's role",
               description = "Changes an existing member's role to MEMBER or VIEWER. "
                           + "The OWNER role cannot be transferred through this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{userId}")
    ResponseEntity<MemberResponse> updateRole(
            @PathVariable long projectId,
            @PathVariable long userId,
            @Valid @RequestBody UpdateMemberRoleRequest req) {
        return ResponseEntity.ok(service.updateRole(projectId, userId, req));
    }

    @Operation(summary = "Remove a member from a project")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    ResponseEntity<Void> remove(
            @PathVariable long projectId,
            @PathVariable long userId) {
        service.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
