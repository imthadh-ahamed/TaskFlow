package com.taskflow.common.security;

import java.security.Principal;

/** Immutable principal placed in the SecurityContext after a validated access token. */
public record AuthenticatedUser(Long userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
