package com.taskflow.auth.application;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        // Generic message — never leak whether the email or the password was wrong.
        super("Invalid credentials");
    }
}
