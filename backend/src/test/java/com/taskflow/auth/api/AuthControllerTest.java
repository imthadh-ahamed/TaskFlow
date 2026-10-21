package com.taskflow.auth.api;

import com.taskflow.auth.application.AuthService;
import com.taskflow.auth.application.CookieService;
import com.taskflow.auth.application.DuplicateEmailException;
import com.taskflow.auth.application.InvalidCredentialsException;
import com.taskflow.auth.application.JwtService;
import com.taskflow.auth.application.LoginResult;
import com.taskflow.auth.application.SessionRevokedException;
import com.taskflow.auth.application.TokenValidationException;
import com.taskflow.common.security.AuthenticatedUser;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @MockitoBean
    CookieService cookieService;

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

    // ---- refresh ----

    @Test
    void refresh_validCookie_returns200WithNewCookies() throws Exception {
        ResponseCookie newAccess = ResponseCookie.from("tf_access", "new-access")
                .httpOnly(true).path("/api").maxAge(900).build();
        ResponseCookie newRefresh = ResponseCookie.from("tf_refresh", "new-refresh")
                .httpOnly(true).path("/api/v1/auth/refresh").maxAge(604800).build();
        when(authService.refresh(anyString())).thenReturn(new LoginResult(
                new AuthResponse(1L, "alice@example.com", "Alice", "user"),
                newAccess, newRefresh));

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("tf_refresh", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedToken_returns401WithSessionRevokedCode() throws Exception {
        when(authService.refresh(anyString())).thenThrow(new SessionRevokedException());

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("tf_refresh", "replayed-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }

    @Test
    void refresh_expiredToken_returns401() throws Exception {
        when(authService.refresh(anyString())).thenThrow(
                new TokenValidationException("Token has expired"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("tf_refresh", "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    // ---- logout ----

    @Test
    void logout_returns204WithClearedCookies() throws Exception {
        when(cookieService.clearAccessCookie()).thenReturn(
                ResponseCookie.from("tf_access", "").path("/api").maxAge(0).build());
        when(cookieService.clearRefreshCookie()).thenReturn(
                ResponseCookie.from("tf_refresh", "").path("/api/v1/auth/refresh").maxAge(0).build());

        mvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("tf_refresh", "some-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void logout_noCookie_stillReturns204() throws Exception {
        when(cookieService.clearAccessCookie()).thenReturn(
                ResponseCookie.from("tf_access", "").path("/api").maxAge(0).build());
        when(cookieService.clearRefreshCookie()).thenReturn(
                ResponseCookie.from("tf_refresh", "").path("/api/v1/auth/refresh").maxAge(0).build());

        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    // ---- me ----

    @Test
    void me_authenticated_returnsUserProfile() throws Exception {
        when(authService.me(1L)).thenReturn(
                new AuthResponse(1L, "alice@example.com", "Alice", "user"));

        var auth = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mvc.perform(get("/api/v1/auth/me").with(req -> { req.setUserPrincipal(auth); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

}
