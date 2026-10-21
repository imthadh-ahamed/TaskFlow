package com.taskflow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defense-in-depth CSRF protection (see docs/auth-design-adr.md).
 * For state-changing methods (POST, PATCH, PUT, DELETE), if an Origin header is present
 * it must match a configured allowed origin. Requests without an Origin header are
 * considered same-origin and are allowed through (SameSite=Strict is the primary CSRF guard).
 */
@Component
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> STATE_CHANGING = Set.of("POST", "PATCH", "PUT", "DELETE");

    private final Set<String> allowedOrigins;

    public OriginCheckFilter(
            @Value("${cors.allowed-origins:http://localhost:3000}") String allowedOriginsConfig
    ) {
        this.allowedOrigins = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        String origin = request.getHeader("Origin");

        if (STATE_CHANGING.contains(method) && origin != null && !allowedOrigins.contains(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: untrusted origin");
            return;
        }

        chain.doFilter(request, response);
    }
}
