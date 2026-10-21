package com.taskflow.common.security;

import com.taskflow.auth.application.JwtService;
import com.taskflow.auth.application.TokenValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reads the {@code tf_access} cookie, validates the JWT, and populates the SecurityContext.
 * Expired or invalid tokens are silently ignored — Spring Security will then reject
 * protected requests downstream with a 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        extractCookie(request, "tf_access").ifPresent(token -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    JwtService.ParsedClaims claims = jwtService.validateAccessToken(token);
                    AuthenticatedUser principal = new AuthenticatedUser(claims.userId());
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (TokenValidationException ignored) {
                    // Invalid or expired — leave SecurityContext unauthenticated.
                    // Spring Security will return 401 for protected endpoints.
                }
            }
        });

        chain.doFilter(request, response);
    }

    private static Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
