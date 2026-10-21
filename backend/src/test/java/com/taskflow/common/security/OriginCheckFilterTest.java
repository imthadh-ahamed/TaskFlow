package com.taskflow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that OriginCheckFilter enforces CSRF-defence:
 * state-changing requests with an untrusted Origin are rejected with 403;
 * requests without an Origin header, or from a trusted origin, pass through.
 */
class OriginCheckFilterTest {

    private static final String TRUSTED_ORIGIN = "http://localhost:3000";

    // Single allowed origin — same as the default prod config
    private OriginCheckFilter filter;

    @BeforeEach
    void setUp() {
        filter = new OriginCheckFilter(TRUSTED_ORIGIN);
    }

    // ---- GET requests are never state-changing, always pass ----

    @Test
    void get_withTrustedOrigin_passes() throws Exception {
        assertPassesFilter("GET", TRUSTED_ORIGIN);
    }

    @Test
    void get_withUntrustedOrigin_passes() throws Exception {
        assertPassesFilter("GET", "http://evil.example.com");
    }

    @Test
    void get_withoutOrigin_passes() throws Exception {
        assertPassesFilter("GET", null);
    }

    // ---- Trusted origin is always allowed for state-changing methods ----

    @Test
    void post_withTrustedOrigin_passes() throws Exception {
        assertPassesFilter("POST", TRUSTED_ORIGIN);
    }

    @Test
    void patch_withTrustedOrigin_passes() throws Exception {
        assertPassesFilter("PATCH", TRUSTED_ORIGIN);
    }

    @Test
    void delete_withTrustedOrigin_passes() throws Exception {
        assertPassesFilter("DELETE", TRUSTED_ORIGIN);
    }

    // ---- Untrusted origins are rejected for all state-changing methods ----

    @Test
    void post_withUntrustedOrigin_returns403() throws Exception {
        assertRejected("POST", "http://attacker.example.com");
    }

    @Test
    void put_withUntrustedOrigin_returns403() throws Exception {
        assertRejected("PUT", "http://attacker.example.com");
    }

    @Test
    void patch_withUntrustedOrigin_returns403() throws Exception {
        assertRejected("PATCH", "http://attacker.example.com");
    }

    @Test
    void delete_withUntrustedOrigin_returns403() throws Exception {
        assertRejected("DELETE", "http://attacker.example.com");
    }

    // ---- No Origin header means same-origin navigation — allowed ----

    @Test
    void post_withoutOriginHeader_passes() throws Exception {
        assertPassesFilter("POST", null);
    }

    @Test
    void delete_withoutOriginHeader_passes() throws Exception {
        assertPassesFilter("DELETE", null);
    }

    // ---- Multiple allowed origins ----

    @Test
    void post_secondAllowedOrigin_passes() throws Exception {
        OriginCheckFilter multiFilter =
                new OriginCheckFilter("http://localhost:3000,https://app.example.com");

        MockHttpServletRequest request = buildRequest("POST", "https://app.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        multiFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    // ---- Helpers ----

    private void assertPassesFilter(String method, String origin) throws Exception {
        MockHttpServletRequest request = buildRequest(method, origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest())
                .as("filter chain should have been called")
                .isNotNull();
    }

    private void assertRejected(String method, String origin) throws Exception {
        MockHttpServletRequest request = buildRequest(method, origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest())
                .as("filter chain should NOT have been called")
                .isNull();
    }

    private static MockHttpServletRequest buildRequest(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }
}
