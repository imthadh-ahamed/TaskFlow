package com.taskflow.auth.domain;

/** Immutable snapshot of a users row for authentication purposes. */
public record UserAccount(
        Long id,
        String email,
        String displayName,
        String passwordHash,
        String role,
        boolean active
) {}
