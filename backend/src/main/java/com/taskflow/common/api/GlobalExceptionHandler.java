package com.taskflow.common.api;

import com.taskflow.auth.application.DuplicateEmailException;
import com.taskflow.auth.application.InvalidCredentialsException;
import com.taskflow.auth.application.SessionRevokedException;
import com.taskflow.auth.application.TokenValidationException;
import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR",
                "Validation failed", details, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        var details = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String field = cv.getPropertyPath().toString();
                    // strip method prefix (e.g. "listProjects.size" → "size")
                    int dot = field.lastIndexOf('.');
                    return new ErrorResponse.FieldError(
                            dot >= 0 ? field.substring(dot + 1) : field,
                            cv.getMessage());
                })
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR",
                "Validation failed", details, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_REQUEST",
                "Request body is malformed or contains an unrecognised value", List.of(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_REQUEST",
                ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_TRANSITION",
                ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "Conflict", "EMAIL_ALREADY_EXISTS",
                ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest req) {
        // Always return a generic message — do not reveal whether the email or password was wrong.
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_CREDENTIALS",
                "Invalid credentials", List.of(), req);
    }

    @ExceptionHandler(SessionRevokedException.class)
    ResponseEntity<ErrorResponse> handleSessionRevoked(
            SessionRevokedException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", "SESSION_REVOKED",
                "Session has been revoked", List.of(), req);
    }

    @ExceptionHandler(TokenValidationException.class)
    ResponseEntity<ErrorResponse> handleTokenValidation(
            TokenValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_TOKEN",
                "Authentication required", List.of(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", "FORBIDDEN",
                "You do not have permission to access this resource", List.of(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "Not Found", "NOT_FOUND",
                ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        // Do not expose constraint names or SQL details
        return error(HttpStatusCode.valueOf(422), "Unprocessable Entity", "DATA_INTEGRITY_ERROR",
                "Request violates a data constraint — verify referenced resources exist", List.of(), req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred", List.of(), req);
    }

    private static ResponseEntity<ErrorResponse> error(
            HttpStatusCode status, String errorPhrase, String code,
            String message, List<ErrorResponse.FieldError> details,
            HttpServletRequest req) {
        String requestId = resolveRequestId(req);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        ErrorResponse body = new ErrorResponse(
                status.value(), errorPhrase, code, message, details,
                Instant.now().toString(), req.getRequestURI());
        return new ResponseEntity<>(body, headers, status);
    }

    private static String resolveRequestId(HttpServletRequest req) {
        String incoming = req.getHeader("X-Request-Id");
        return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
    }
}
