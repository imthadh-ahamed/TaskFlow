package com.taskflow.membership.api;

import com.taskflow.auth.application.JwtService;
import com.taskflow.common.ResourceNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import com.taskflow.common.api.GlobalExceptionHandler;
import com.taskflow.membership.application.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@WebMvcTest({MemberController.class, GlobalExceptionHandler.class})
class MemberControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean MemberService service;
    @MockitoBean JwtService jwtService;

    private MemberResponse sampleMember(long userId, String role) {
        return new MemberResponse(1L, userId, role, "2024-01-01T00:00:00Z");
    }

    // --- POST /api/v1/projects/{projectId}/members ---

    @Test
    void add_returns_201() throws Exception {
        when(service.addMember(eq(1L), any())).thenReturn(sampleMember(5L, "member"));

        mvc.perform(post("/api/v1/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"role":"MEMBER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void add_returns_400_when_role_missing() throws Exception {
        mvc.perform(post("/api/v1/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void add_returns_400_for_unknown_role_value() throws Exception {
        mvc.perform(post("/api/v1/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void add_returns_400_when_service_rejects_owner_role() throws Exception {
        when(service.addMember(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Cannot assign role OWNER via API"));

        mvc.perform(post("/api/v1/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"role":"OWNER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void add_returns_404_when_project_not_found() throws Exception {
        when(service.addMember(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Project 99 not found"));

        mvc.perform(post("/api/v1/projects/99/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":5,"role":"MEMBER"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // --- GET /api/v1/projects/{projectId}/members ---

    @Test
    void list_returns_member_list() throws Exception {
        when(service.listMembers(1L)).thenReturn(
                List.of(sampleMember(10L, "owner"), sampleMember(5L, "member")));

        mvc.perform(get("/api/v1/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("owner"))
                .andExpect(jsonPath("$[1].role").value("member"));
    }

    // --- PATCH /api/v1/projects/{projectId}/members/{userId} ---

    @Test
    void updateRole_returns_200() throws Exception {
        when(service.updateRole(eq(1L), eq(5L), any()))
                .thenReturn(sampleMember(5L, "viewer"));

        mvc.perform(patch("/api/v1/projects/1/members/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("viewer"));
    }

    @Test
    void updateRole_returns_400_when_modifying_owner() throws Exception {
        when(service.updateRole(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Cannot change the owner's role"));

        mvc.perform(patch("/api/v1/projects/1/members/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"MEMBER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void updateRole_returns_404_when_member_not_found() throws Exception {
        when(service.updateRole(eq(1L), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Member 99 not found in project 1"));

        mvc.perform(patch("/api/v1/projects/1/members/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // --- DELETE /api/v1/projects/{projectId}/members/{userId} ---

    @Test
    void remove_returns_204() throws Exception {
        doNothing().when(service).removeMember(1L, 5L);

        mvc.perform(delete("/api/v1/projects/1/members/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void remove_returns_400_when_removing_owner() throws Exception {
        doThrow(new IllegalArgumentException("Cannot remove the project owner"))
                .when(service).removeMember(1L, 10L);

        mvc.perform(delete("/api/v1/projects/1/members/10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void remove_returns_404_when_member_not_found() throws Exception {
        doThrow(new ResourceNotFoundException("Member 99 not found in project 1"))
                .when(service).removeMember(1L, 99L);

        mvc.perform(delete("/api/v1/projects/1/members/99"))
                .andExpect(status().isNotFound());
    }
}
