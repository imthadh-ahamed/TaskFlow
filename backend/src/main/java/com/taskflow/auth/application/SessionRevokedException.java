package com.taskflow.auth.application;

/**
 * Thrown when a refresh token has been revoked, indicating possible replay/reuse.
 * On reuse detection the entire token family is revoked before this is thrown.
 * Always maps to a generic 401 — never disclose the revocation reason to callers.
 */
public class SessionRevokedException extends RuntimeException {
    public SessionRevokedException() {
        super("Session revoked");
    }
}
