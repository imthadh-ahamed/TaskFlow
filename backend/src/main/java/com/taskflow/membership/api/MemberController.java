package com.taskflow.membership.api;

import com.taskflow.membership.application.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
class MemberController {

    private final MemberService service;

    MemberController(MemberService service) {
        this.service = service;
    }

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

    @GetMapping
    ResponseEntity<List<MemberResponse>> list(@PathVariable long projectId) {
        return ResponseEntity.ok(service.listMembers(projectId));
    }

    @PatchMapping("/{userId}")
    ResponseEntity<MemberResponse> updateRole(
            @PathVariable long projectId,
            @PathVariable long userId,
            @Valid @RequestBody UpdateMemberRoleRequest req) {
        return ResponseEntity.ok(service.updateRole(projectId, userId, req));
    }

    @DeleteMapping("/{userId}")
    ResponseEntity<Void> remove(
            @PathVariable long projectId,
            @PathVariable long userId) {
        service.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
