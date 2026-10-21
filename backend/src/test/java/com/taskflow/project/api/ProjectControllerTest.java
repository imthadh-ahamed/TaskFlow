package com.taskflow.project.api;

import com.taskflow.auth.application.JwtService;
import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.common.security.WithAuthenticatedUser;
import com.taskflow.project.application.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithAuthenticatedUser
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProjectService service;
    @MockitoBean
    JwtService jwtService;

    private ProjectResponse sample(long id) {
        return new ProjectResponse(id, 1L, "Alpha", "desc",
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
    }

    // --- POST /api/v1/projects ---

    @Test
    void create_returns_201_with_location() throws Exception {
        when(service.createProject(any(), eq(1L))).thenReturn(sample(42L));

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alpha","description":"desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/projects/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Alpha"));
    }

    @Test
    void create_returns_400_when_name_is_blank() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    // --- GET /api/v1/projects ---

    @Test
    void list_returns_200_with_page() throws Exception {
        var page = PageResponse.of(List.of(sample(1L), sample(2L)), 0, 20, 2L);
        when(service.listProjects(0, 20, 1L)).thenReturn(page);

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_returns_400_when_size_exceeds_max() throws Exception {
        mockMvc.perform(get("/api/v1/projects").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_returns_400_when_size_is_zero() throws Exception {
        mockMvc.perform(get("/api/v1/projects").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_returns_400_when_page_is_negative() throws Exception {
        mockMvc.perform(get("/api/v1/projects").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/v1/projects/{id} ---

    @Test
    void getOne_returns_200() throws Exception {
        when(service.getProject(1L, 1L)).thenReturn(sample(1L));

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getOne_returns_404_when_not_found() throws Exception {
        when(service.getProject(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Project 99 not found"));

        mockMvc.perform(get("/api/v1/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project 99 not found"));
    }

    @Test
    void getOne_returns_403_when_not_a_member() throws Exception {
        when(service.getProject(2L, 1L)).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/v1/projects/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // --- PATCH /api/v1/projects/{id} ---

    @Test
    void update_returns_200() throws Exception {
        when(service.updateProject(eq(1L), any(), eq(1L))).thenReturn(sample(1L));

        mockMvc.perform(patch("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns_404_when_not_found() throws Exception {
        when(service.updateProject(eq(99L), any(), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Project 99 not found"));

        mockMvc.perform(patch("/api/v1/projects/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X"}
                                """))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/v1/projects/{id} ---

    @Test
    void delete_returns_204() throws Exception {
        doNothing().when(service).deleteProject(1L, 1L);

        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns_404_when_not_found() throws Exception {
        doThrow(new ResourceNotFoundException("Project 99 not found"))
                .when(service).deleteProject(99L, 1L);

        mockMvc.perform(delete("/api/v1/projects/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns_403_when_not_owner() throws Exception {
        doThrow(new ForbiddenException()).when(service).deleteProject(2L, 1L);

        mockMvc.perform(delete("/api/v1/projects/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
