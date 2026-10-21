package com.taskflow.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe user/session metadata returned after registration or login. Token values are never included. */
@Schema(description = "Authenticated user metadata. Token values are never returned in the response body.")
public record AuthResponse(Long id, String email, String displayName, String role) {}
