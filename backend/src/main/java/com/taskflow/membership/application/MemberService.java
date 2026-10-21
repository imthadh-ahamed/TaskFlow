package com.taskflow.membership.application;

import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.membership.api.AddMemberRequest;
import com.taskflow.membership.api.MemberResponse;
import com.taskflow.membership.api.UpdateMemberRoleRequest;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MemberService {

    private final JdbcMemberRepository memberRepository;
    private final JdbcProjectRepository projectRepository;

    public MemberService(JdbcMemberRepository memberRepository,
                         JdbcProjectRepository projectRepository) {
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
    }

    public MemberResponse addMember(long projectId, AddMemberRequest req, long callerId) {
        requireProjectExists(projectId);
        requireOwner(projectId, callerId);
        if (req.role() == MemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign role OWNER via API");
        }
        ProjectMember added = memberRepository.add(projectId, req.userId(), req.role());
        return toResponse(added);
    }

    public List<MemberResponse> listMembers(long projectId, long callerId) {
        requireProjectExists(projectId);
        requireMember(projectId, callerId);
        return memberRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MemberResponse updateRole(long projectId, long userId, UpdateMemberRoleRequest req, long callerId) {
        requireProjectExists(projectId);
        requireOwner(projectId, callerId);
        ProjectMember existing = memberRepository.findByProjectAndUser(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member " + userId + " not found in project " + projectId));
        if (existing.role() == MemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the owner's role");
        }
        if (req.role() == MemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign role OWNER via API");
        }
        return memberRepository.updateRole(projectId, userId, req.role())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member " + userId + " not found in project " + projectId));
    }

    public void removeMember(long projectId, long userId, long callerId) {
        requireProjectExists(projectId);
        requireOwner(projectId, callerId);
        ProjectMember existing = memberRepository.findByProjectAndUser(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member " + userId + " not found in project " + projectId));
        if (existing.role() == MemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the project owner");
        }
        memberRepository.remove(projectId, userId);
    }

    private void requireProjectExists(long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project " + projectId + " not found"));
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

    private MemberResponse toResponse(ProjectMember m) {
        return new MemberResponse(
                m.projectId(),
                m.userId(),
                m.role().name().toLowerCase(),
                m.createdAt().toString()
        );
    }
}
