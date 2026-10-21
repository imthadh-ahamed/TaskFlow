package com.taskflow.membership.application;

import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.membership.api.AddMemberRequest;
import com.taskflow.membership.api.MemberResponse;
import com.taskflow.membership.api.UpdateMemberRoleRequest;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock JdbcMemberRepository memberRepository;
    @Mock JdbcProjectRepository projectRepository;
    @InjectMocks MemberService service;

    private Project sampleProject() {
        return new Project(1L, 10L, "P", null, Instant.EPOCH, Instant.EPOCH);
    }

    private ProjectMember member(long userId, MemberRole role) {
        return new ProjectMember(1L, userId, role, Instant.EPOCH);
    }

    // --- addMember ---

    @Test
    void addMember_succeeds_for_valid_role() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.add(1L, 5L, MemberRole.MEMBER)).thenReturn(member(5L, MemberRole.MEMBER));

        MemberResponse resp = service.addMember(1L, new AddMemberRequest(5L, MemberRole.MEMBER));

        assertThat(resp.userId()).isEqualTo(5L);
        assertThat(resp.role()).isEqualTo("member");
    }

    @Test
    void addMember_throws_when_project_not_found() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.addMember(99L, new AddMemberRequest(5L, MemberRole.MEMBER)));
    }

    @Test
    void addMember_rejects_owner_role() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.addMember(1L, new AddMemberRequest(5L, MemberRole.OWNER)))
                .withMessageContaining("OWNER");
    }

    // --- listMembers ---

    @Test
    void listMembers_returns_all_members() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectId(1L)).thenReturn(
                List.of(member(10L, MemberRole.OWNER), member(5L, MemberRole.MEMBER)));

        List<MemberResponse> list = service.listMembers(1L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).role()).isEqualTo("owner");
    }

    // --- updateRole ---

    @Test
    void updateRole_changes_member_to_viewer() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 5L))
                .thenReturn(Optional.of(member(5L, MemberRole.MEMBER)));
        when(memberRepository.updateRole(1L, 5L, MemberRole.VIEWER))
                .thenReturn(Optional.of(member(5L, MemberRole.VIEWER)));

        MemberResponse resp = service.updateRole(1L, 5L, new UpdateMemberRoleRequest(MemberRole.VIEWER));

        assertThat(resp.role()).isEqualTo("viewer");
    }

    @Test
    void updateRole_rejects_changing_owner_role() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 10L))
                .thenReturn(Optional.of(member(10L, MemberRole.OWNER)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.updateRole(1L, 10L,
                        new UpdateMemberRoleRequest(MemberRole.MEMBER)))
                .withMessageContaining("owner");
    }

    @Test
    void updateRole_rejects_assigning_owner_role() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 5L))
                .thenReturn(Optional.of(member(5L, MemberRole.MEMBER)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.updateRole(1L, 5L,
                        new UpdateMemberRoleRequest(MemberRole.OWNER)))
                .withMessageContaining("OWNER");
    }

    @Test
    void updateRole_throws_when_member_not_found() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.updateRole(1L, 99L,
                        new UpdateMemberRoleRequest(MemberRole.VIEWER)));
    }

    // --- removeMember ---

    @Test
    void removeMember_deletes_non_owner() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 5L))
                .thenReturn(Optional.of(member(5L, MemberRole.MEMBER)));
        when(memberRepository.remove(1L, 5L)).thenReturn(true);

        assertThatCode(() -> service.removeMember(1L, 5L)).doesNotThrowAnyException();
        verify(memberRepository).remove(1L, 5L);
    }

    @Test
    void removeMember_rejects_removing_owner() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 10L))
                .thenReturn(Optional.of(member(10L, MemberRole.OWNER)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.removeMember(1L, 10L))
                .withMessageContaining("owner");
    }

    @Test
    void removeMember_throws_when_member_not_found() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(memberRepository.findByProjectAndUser(1L, 99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.removeMember(1L, 99L));
    }
}
