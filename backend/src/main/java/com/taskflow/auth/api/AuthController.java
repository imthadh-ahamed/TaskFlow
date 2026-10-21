package com.taskflow.auth.api;

import com.taskflow.auth.application.AuthService;
import com.taskflow.auth.application.CookieService;
import com.taskflow.auth.application.LoginResult;
import com.taskflow.common.api.ErrorResponse;
import com.taskflow.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Tag(name = "Auth", description = "Registration, login, and session management")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "Register a new account",
               description = "Creates a new user account. Does not issue tokens — call /login after registration.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse body = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Log in",
               description = "Authenticates with email and password. On success sets HttpOnly cookies "
                           + "`tf_access` (15 min) and `tf_refresh` (7 days). "
                           + "Token values are never returned in the response body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated; cookies set"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResult result = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.accessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.response());
    }

    @Operation(summary = "Rotate refresh token",
               description = "Validates the tf_refresh cookie, issues a new access + refresh cookie pair, "
                           + "and revokes the consumed refresh token. "
                           + "Presenting an already-revoked token revokes the entire session family.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued; cookies rotated"),
            @ApiResponse(responseCode = "401", description = "Missing, expired, revoked, or invalid refresh token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String token = extractCookie(request, CookieService.REFRESH_COOKIE);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LoginResult result = authService.refresh(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.accessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.response());
    }

    @Operation(summary = "Log out",
               description = "Revokes the active refresh session and clears both auth cookies. "
                           + "Succeeds even if the refresh cookie is absent or already expired.")
    @ApiResponse(responseCode = "204", description = "Session revoked; cookies cleared")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractCookie(request, CookieService.REFRESH_COOKIE);
        authService.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.clearRefreshCookie().toString())
                .build();
    }

    @Operation(summary = "Current user",
               description = "Returns the authenticated user's profile. Requires a valid tf_access cookie.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user metadata"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.me(principal.userId()));
    }

    private static String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
