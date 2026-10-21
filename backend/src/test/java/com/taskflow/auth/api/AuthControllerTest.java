package com.taskflow.auth.api;

import com.taskflow.auth.application.AuthService;
import com.taskflow.auth.application.DuplicateEmailException;
import com.taskflow.auth.application.InvalidCredentialsException;
import com.taskflow.auth.application.JwtService;
import com.taskflow.auth.application.LoginResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for AuthController.
 * /api/v1/auth/** is permitAll() in SecurityConfig so no custom security override is needed.
 * JwtService is mocked so JwtAuthenticationFilter can be instantiated without a real secret.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    JwtService jwtService;

    // ---- register ----

    @Test
    void register_validRequest_returns201AndUserBody() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthResponse(1L, "alice@example.com", "Alice", "user"));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"secure123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("user"))
                // Token values must never appear in the response body
                .andExpect(content().string(not(containsString("eyJ"))));
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_passwordTooShort_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateEmailException("Email already in use"));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"secure123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    // ---- login ----

    @Test
    void login_validCredentials_returns200WithCookiesAndUserBody() throws Exception {
        ResponseCookie accessCookie = ResponseCookie.from("tf_access", "access-token-value")
                .httpOnly(true).sameSite("Strict").path("/api").maxAge(900).build();
        ResponseCookie refreshCookie = ResponseCookie.from("tf_refresh", "refresh-token-value")
                .httpOnly(true).sameSite("Strict").path("/api/v1/auth/refresh").maxAge(604800).build();
        when(authService.login(any())).thenReturn(new LoginResult(
                new AuthResponse(1L, "alice@example.com", "Alice", "user"),
                accessCookie, refreshCookie));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"secure123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                // Cookies must be present
                .andExpect(header().exists("Set-Cookie"))
                // Token values must never appear in the JSON body
                .andExpect(content().string(not(containsString("access-token-value"))))
                .andExpect(content().string(not(containsString("refresh-token-value"))));
    }

    @Test
    void login_invalidCredentials_returns401WithGenericMessage() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                // Generic message — must not mention email or password specifically
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"secure123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
