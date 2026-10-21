package com.taskflow.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

/** Immutable principal placed in the SecurityContext after a validated access token. */
public record AuthenticatedUser(Long userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }

    public static long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u.userId();
        }
        throw new IllegalStateException("No authenticated user in security context");
    }
}
