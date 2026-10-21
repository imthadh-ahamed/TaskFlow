package com.taskflow.comment.api;

import com.taskflow.auth.application.JwtService;
import com.taskflow.comment.application.CommentService;
import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.GlobalExceptionHandler;
import com.taskflow.common.api.PageResponse;
import com.taskflow.common.security.WithAuthenticatedUser;
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

@WithAuthenticatedUser
@WebMvcTest({CommentController.class, GlobalExceptionHandler.class})
class CommentControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CommentService service;
    @MockitoBean JwtService jwtService;

    private CommentResponse sampleComment(long id) {
        return new CommentResponse(id, 1L, 1L, "Looks good",
                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    }

    // --- POST /api/v1/tasks/{taskId}/comments ---

    @Test
    void create_returns_201() throws Exception {
        when(service.createComment(eq(1L), any(), eq(1L))).thenReturn(sampleComment(10L));

        mvc.perform(post("/api/v1/tasks/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Looks good"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.body").value("Looks good"))
                .andExpect(jsonPath("$.authorId").value(1));
    }

    @Test
    void create_returns_400_when_body_blank() throws Exception {
        mvc.perform(post("/api/v1/tasks/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns_403_when_viewer_tries_to_comment() throws Exception {
        when(service.createComment(eq(1L), any(), eq(1L))).thenThrow(new ForbiddenException());

        mvc.perform(post("/api/v1/tasks/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Looks good"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_returns_404_when_task_not_found() throws Exception {
        when(service.createComment(eq(99L), any(), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Task 99 not found"));

        mvc.perform(post("/api/v1/tasks/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Looks good"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // --- GET /api/v1/tasks/{taskId}/comments ---

    @Test
    void list_returns_paged_results() throws Exception {
        PageResponse<CommentResponse> page = PageResponse.of(
                List.of(sampleComment(1L)), 0, 20, 1L);
        when(service.listComments(eq(1L), eq(0), eq(20), eq(1L))).thenReturn(page);

        mvc.perform(get("/api/v1/tasks/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void list_returns_404_when_task_not_found() throws Exception {
        when(service.listComments(eq(99L), anyInt(), anyInt(), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Task 99 not found"));

        mvc.perform(get("/api/v1/tasks/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void list_returns_400_for_invalid_page_params() throws Exception {
        mvc.perform(get("/api/v1/tasks/1/comments?size=0"))
                .andExpect(status().isBadRequest());
    }
}
