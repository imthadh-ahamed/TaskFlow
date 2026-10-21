package com.taskflow.common.api;

import com.taskflow.auth.application.JwtService;
import com.taskflow.common.security.WithAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithAuthenticatedUser
@WebMvcTest({ ExceptionHandlerTestController.class, GlobalExceptionHandler.class })
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;

    // --- 400 VALIDATION_ERROR ---

    @Test
    void validation_returns_400_with_field_details() throws Exception {
        mvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 400 INVALID_REQUEST (malformed body) ---

    @Test
    void malformed_body_returns_400_invalid_request() throws Exception {
        mvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 400 INVALID_REQUEST (illegal argument) ---

    @Test
    void illegal_argument_returns_400_invalid_request() throws Exception {
        mvc.perform(get("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("bad argument value"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 400 INVALID_TRANSITION (illegal state) ---

    @Test
    void illegal_state_returns_400_invalid_transition() throws Exception {
        mvc.perform(get("/test/illegal-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Cannot transition from TODO to DONE"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 403 FORBIDDEN ---

    @Test
    void forbidden_returns_403() throws Exception {
        mvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 404 NOT_FOUND ---

    @Test
    void not_found_returns_404() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource 42 not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 422 DATA_INTEGRITY_ERROR ---

    @Test
    void data_integrity_returns_422() throws Exception {
        mvc.perform(get("/test/data-integrity"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_ERROR"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- 500 INTERNAL_ERROR ---

    @Test
    void general_exception_returns_500_without_internal_details() throws Exception {
        mvc.perform(get("/test/general"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // --- X-Request-Id pass-through ---

    @Test
    void request_id_in_request_header_is_echoed_in_response() throws Exception {
        mvc.perform(get("/test/not-found")
                        .header("X-Request-Id", "my-trace-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "my-trace-123"));
    }

    // --- Response shape: stable fields always present ---

    @Test
    void error_response_contains_all_stable_fields() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists());
    }
}
