package com.taskflow.auth.application;

import com.taskflow.auth.api.AuthResponse;
import org.springframework.http.ResponseCookie;

/** Value returned by AuthService.login — carries the response body and the two cookies to set. */
public record LoginResult(
        AuthResponse response,
        ResponseCookie accessCookie,
        ResponseCookie refreshCookie
) {}
