package com.taskflow.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceTest {

    // cookie.secure=false for local dev; true in production
    private final CookieService insecure = new CookieService(false, 15L, 7L);
    private final CookieService secure   = new CookieService(true,  15L, 7L);

    // ---- access cookie ----

    @Test
    void buildAccessCookie_isHttpOnly() {
        assertThat(insecure.buildAccessCookie("tok").isHttpOnly()).isTrue();
    }

    @Test
    void buildAccessCookie_hasSameSiteStrict() {
        assertThat(insecure.buildAccessCookie("tok").getSameSite()).isEqualTo("Strict");
    }

    @Test
    void buildAccessCookie_hasApiPath() {
        assertThat(insecure.buildAccessCookie("tok").getPath()).isEqualTo("/api");
    }

    @Test
    void buildAccessCookie_maxAgeMatchesTtl() {
        // 15 minutes × 60 seconds
        assertThat(insecure.buildAccessCookie("tok").getMaxAge().getSeconds()).isEqualTo(900L);
    }

    @Test
    void buildAccessCookie_secureFlagFalseInDevMode() {
        assertThat(insecure.buildAccessCookie("tok").isSecure()).isFalse();
    }

    @Test
    void buildAccessCookie_secureFlagTrueInProductionMode() {
        assertThat(secure.buildAccessCookie("tok").isSecure()).isTrue();
    }

    @Test
    void buildAccessCookie_nameIsTfAccess() {
        assertThat(insecure.buildAccessCookie("tok").getName()).isEqualTo("tf_access");
    }

    // ---- refresh cookie ----

    @Test
    void buildRefreshCookie_isHttpOnly() {
        assertThat(insecure.buildRefreshCookie("tok").isHttpOnly()).isTrue();
    }

    @Test
    void buildRefreshCookie_hasSameSiteStrict() {
        assertThat(insecure.buildRefreshCookie("tok").getSameSite()).isEqualTo("Strict");
    }

    @Test
    void buildRefreshCookie_hasRefreshPath() {
        assertThat(insecure.buildRefreshCookie("tok").getPath()).isEqualTo("/api/v1/auth/refresh");
    }

    @Test
    void buildRefreshCookie_maxAgeMatchesTtl() {
        // 7 days × 86 400 seconds
        assertThat(insecure.buildRefreshCookie("tok").getMaxAge().getSeconds()).isEqualTo(604800L);
    }

    @Test
    void buildRefreshCookie_nameIsTfRefresh() {
        assertThat(insecure.buildRefreshCookie("tok").getName()).isEqualTo("tf_refresh");
    }

    // ---- clear cookies ----

    @Test
    void clearAccessCookie_hasMaxAgeZero() {
        ResponseCookie c = insecure.clearAccessCookie();
        assertThat(c.getMaxAge().getSeconds()).isZero();
        assertThat(c.getValue()).isEmpty();
    }

    @Test
    void clearRefreshCookie_hasMaxAgeZeroAndRefreshPath() {
        ResponseCookie c = insecure.clearRefreshCookie();
        assertThat(c.getMaxAge().getSeconds()).isZero();
        assertThat(c.getPath()).isEqualTo("/api/v1/auth/refresh");
    }

    // ---- path scoping prevents cross-endpoint cookie leakage ----

    @Test
    void accessAndRefreshCookies_haveDifferentPaths() {
        assertThat(insecure.buildAccessCookie("a").getPath())
                .isNotEqualTo(insecure.buildRefreshCookie("r").getPath());
    }
}
