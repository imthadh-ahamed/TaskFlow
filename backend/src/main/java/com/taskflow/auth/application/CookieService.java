package com.taskflow.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Builds HttpOnly, SameSite=Strict cookies for access and refresh tokens.
 * The Secure flag is controlled by {@code cookie.secure} (true in production, false for local HTTP).
 */
@Service
public class CookieService {

    public static final String ACCESS_COOKIE = "tf_access";
    public static final String REFRESH_COOKIE = "tf_refresh";
    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";
    private static final String SAME_SITE = "Strict";

    private final boolean secure;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    CookieService(
            @Value("${cookie.secure:true}") boolean secure,
            @Value("${jwt.access-token-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${jwt.refresh-token-ttl-days:7}") long refreshTtlDays
    ) {
        this.secure = secure;
        this.accessTtlSeconds = accessTtlMinutes * 60L;
        this.refreshTtlSeconds = refreshTtlDays * 86_400L;
    }

    public ResponseCookie buildAccessCookie(String token) {
        return base(ACCESS_COOKIE, token, ACCESS_PATH, accessTtlSeconds).build();
    }

    public ResponseCookie buildRefreshCookie(String token) {
        return base(REFRESH_COOKIE, token, REFRESH_PATH, refreshTtlSeconds).build();
    }

    public ResponseCookie clearAccessCookie() {
        return base(ACCESS_COOKIE, "", ACCESS_PATH, 0).build();
    }

    public ResponseCookie clearRefreshCookie() {
        return base(REFRESH_COOKIE, "", REFRESH_PATH, 0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(maxAge);
    }
}
