package com.taskflow.task.api;

import com.taskflow.auth.application.JwtService;
import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.GlobalExceptionHandler;
import com.taskflow.common.api.PageResponse;
import com.taskflow.common.security.WithAuthenticatedUser;
import com.taskflow.task.application.TaskService;
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
@WebMvcTest({ TaskController.class, GlobalExceptionHandler.class })
class TaskControllerTest {

        @Autowired
        MockMvc mvc;
        @MockitoBean
        TaskService service;
        @MockitoBean
        JwtService jwtService;

        private TaskResponse sampleResponse(long id, String status) {
                return new TaskResponse(id, 1L, null, "Fix bug", null,
                                status, "medium", null,
                                "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        }

        // --- POST /api/v1/projects/{projectId}/tasks ---

        @Test
        void create_returns_201_with_location() throws Exception {
                when(service.createTask(eq(1L), any(), eq(1L))).thenReturn(sampleResponse(10L, "todo"));

                mvc.perform(post("/api/v1/projects/1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":"Fix bug"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location",
                                                org.hamcrest.Matchers.endsWith("/api/v1/tasks/10")))
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.status").value("todo"));
        }

        @Test
        void create_returns_400_when_title_blank() throws Exception {
                mvc.perform(post("/api/v1/projects/1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":""}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void create_returns_400_for_unknown_enum_value() throws Exception {
                mvc.perform(post("/api/v1/projects/1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":"Fix bug","priority":"CRITICAL"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }

        @Test
        void create_returns_404_when_project_not_found() throws Exception {
                when(service.createTask(eq(99L), any(), eq(1L)))
                                .thenThrow(new ResourceNotFoundException("Project 99 not found"));

                mvc.perform(post("/api/v1/projects/99/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":"Fix bug"}
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void create_returns_403_when_viewer_tries_to_create() throws Exception {
                when(service.createTask(eq(1L), any(), eq(1L))).thenThrow(new ForbiddenException());

                mvc.perform(post("/api/v1/projects/1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":"Fix bug"}
                                                """))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        // --- GET /api/v1/projects/{projectId}/tasks ---

        @Test
        void list_returns_paged_results() throws Exception {
                PageResponse<TaskResponse> page = PageResponse.of(
                                List.of(sampleResponse(1L, "todo")), 0, 20, 1L);
                when(service.listTasks(eq(1L), isNull(), isNull(), isNull(), eq(0), eq(20), eq(1L)))
                                .thenReturn(page);

                mvc.perform(get("/api/v1/projects/1/tasks"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.total").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void list_returns_400_for_invalid_page_params() throws Exception {
                mvc.perform(get("/api/v1/projects/1/tasks?page=-1"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void list_returns_400_when_size_exceeds_max() throws Exception {
                mvc.perform(get("/api/v1/projects/1/tasks?size=101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void list_returns_400_when_size_is_zero() throws Exception {
                mvc.perform(get("/api/v1/projects/1/tasks?size=0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void list_returns_400_for_invalid_status_filter() throws Exception {
                when(service.listTasks(eq(1L), eq("NONSENSE"), isNull(), isNull(), eq(0), eq(20), eq(1L)))
                                .thenThrow(new IllegalArgumentException(
                                                "Invalid status filter 'NONSENSE'. Valid values: todo, in_progress, in_review, done"));

                mvc.perform(get("/api/v1/projects/1/tasks?status=NONSENSE"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }

        @Test
        void list_returns_400_for_invalid_priority_filter() throws Exception {
                when(service.listTasks(eq(1L), isNull(), eq("NONSENSE"), isNull(), eq(0), eq(20), eq(1L)))
                                .thenThrow(new IllegalArgumentException(
                                                "Invalid priority filter 'NONSENSE'. Valid values: low, medium, high, urgent"));

                mvc.perform(get("/api/v1/projects/1/tasks?priority=NONSENSE"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }

        // --- GET /api/v1/tasks/{taskId} ---

        @Test
        void getOne_returns_200() throws Exception {
                when(service.getTask(1L, 1L)).thenReturn(sampleResponse(1L, "todo"));

                mvc.perform(get("/api/v1/tasks/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        void getOne_returns_404_when_not_found() throws Exception {
                when(service.getTask(99L, 1L)).thenThrow(new ResourceNotFoundException("Task 99 not found"));

                mvc.perform(get("/api/v1/tasks/99"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                                .andExpect(jsonPath("$.message").value("Task 99 not found"));
        }

        // --- PATCH /api/v1/tasks/{taskId} ---

        @Test
        void update_returns_200() throws Exception {
                when(service.updateTask(eq(1L), any(), eq(1L))).thenReturn(sampleResponse(1L, "todo"));

                mvc.perform(patch("/api/v1/tasks/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title":"Updated title"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        void update_returns_400_when_title_too_long() throws Exception {
                String longTitle = "\"" + "a".repeat(256) + "\"";
                mvc.perform(patch("/api/v1/tasks/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":" + longTitle + "}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        // --- PATCH /api/v1/tasks/{taskId}/status ---

        @Test
        void updateStatus_returns_200_on_valid_transition() throws Exception {
                when(service.updateStatus(eq(1L), any(), eq(1L))).thenReturn(sampleResponse(1L, "in_progress"));

                mvc.perform(patch("/api/v1/tasks/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"status":"IN_PROGRESS"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("in_progress"));
        }

        @Test
        void updateStatus_returns_400_when_status_null() throws Exception {
                mvc.perform(patch("/api/v1/tasks/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void updateStatus_returns_400_on_invalid_transition() throws Exception {
                when(service.updateStatus(eq(1L), any(), eq(1L)))
                                .thenThrow(new IllegalStateException("Cannot transition task from TODO to DONE"));

                mvc.perform(patch("/api/v1/tasks/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"status":"DONE"}
                                                """))
                                .andExpect(status().isInternalServerError());
        }
}
